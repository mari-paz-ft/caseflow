package com.caseflow.application.usecase

import com.caseflow.domain.enums.*
import com.caseflow.domain.model.CaseRequest
import com.caseflow.domain.model.ProcessingJob
import com.caseflow.domain.exception.TechnicalFailureException
import com.caseflow.domain.model.ProcessingResult
import com.caseflow.domain.rule.PdfDocumentRules
import com.caseflow.domain.rule.TechnicalRetryPolicy
import com.caseflow.application.port.CaseRequestRepository
import com.caseflow.application.port.DocumentStoragePort
import com.caseflow.application.port.ProcessingJobRepository
import com.caseflow.application.port.ProcessingResultRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class AnalysisEngineService(
    private val caseRequestRepository: CaseRequestRepository,
    private val processingJobRepository: ProcessingJobRepository,
    private val processingResultRepository: ProcessingResultRepository,
    private val historyService: HistoryService,
    private val notificationService: NotificationService,
    private val objectMapper: ObjectMapper,
    private val documentStoragePort: DocumentStoragePort
) {
    private val logger = LoggerFactory.getLogger(AnalysisEngineService::class.java)

    @Transactional
    fun processClaimedJob(jobId: UUID, leaseToken: UUID) {
        val job = processingJobRepository.findByIdForUpdate(jobId).orElse(null) ?: return
        val leaseUntil = job.leaseUntil
        if (job.state != JobState.RUNNING || job.leaseToken != leaseToken || leaseUntil?.isAfter(LocalDateTime.now()) != true) {
            return
        }

        try {
            // Simulate brief asynchronous processing delay for realism
            Thread.sleep(1200)
            executeAnalysis(job)
        } catch (ex: Exception) {
            logger.error("Erro durante processamento do job ${job.id}: ${ex.message}", ex)
            markTechnicalFailure(job, "Falha técnica inesperada no executor: ${ex.message}")
        }
    }

    @Transactional
    fun executeAnalysis(job: ProcessingJob): ProcessingResult {
        val caseRequest = job.caseRequest
        logger.info("Iniciando conferência documental da solicitação ${caseRequest.protocol} (Run: ${job.runNumber})")

        job.state = JobState.RUNNING
        job.updatedAt = LocalDateTime.now()
        processingJobRepository.save(job)

        if (caseRequest.status != CaseStatus.PROCESSANDO) {
            caseRequest.status = CaseStatus.PROCESSANDO
            caseRequest.updatedAt = LocalDateTime.now()
            caseRequestRepository.save(caseRequest)
            historyService.record(
                caseRequest = caseRequest,
                eventType = "PROCESSAMENTO_INICIADO",
                actorSubject = "SYSTEM",
                details = "Executor interno assumiu execução #${job.runNumber}"
            )
        }

        // Verificação de simulação explícita de falha técnica
        if (caseRequest.title.contains("[SIMULAR_FALHA]", ignoreCase = true) ||
            caseRequest.description.contains("[SIMULAR_FALHA]", ignoreCase = true)
        ) {
            return markTechnicalFailure(job, "Simulação de indisponibilidade de I/O de armazenamento")
        }

        caseRequest.documents.filter { it.uploadState == UploadState.READY }.forEach { document ->
            val content = documentStoragePort.read(document.storageKey)
            if (!PdfDocumentRules.isAcceptableUpload(document.contentType, content) ||
                content.size.toLong() != document.fileSize ||
                PdfDocumentRules.sha256(content) != document.sha256
            ) {
                throw TechnicalFailureException("Documento armazenado ausente, inválido ou com integridade divergente")
            }
        }

        val reasonCodes = mutableListOf<String>()
        val referenceDate = caseRequest.submittedAt?.toLocalDate() ?: LocalDate.now(ZoneOffset.UTC)

        // Critério 1: Documento de Identificação
        val docIdentificacao = caseRequest.documents.find {
            it.category == DocumentCategory.IDENTIFICACAO && it.uploadState == UploadState.READY
        }
        if (docIdentificacao == null) {
            reasonCodes.add("FALTA_IDENTIFICACAO")
        } else if (docIdentificacao.validUntil != null && docIdentificacao.validUntil!!.isBefore(referenceDate)) {
            reasonCodes.add("DOCUMENTO_VENCIDO_IDENTIFICACAO")
        }

        // Critério 2: Comprovante de Endereço
        val docEndereco = caseRequest.documents.find {
            it.category == DocumentCategory.COMPROVANTE_ENDERECO && it.uploadState == UploadState.READY
        }
        if (docEndereco == null) {
            reasonCodes.add("FALTA_COMPROVANTE_ENDERECO")
        } else if (docEndereco.validUntil != null && docEndereco.validUntil!!.isBefore(referenceDate)) {
            reasonCodes.add("DOCUMENTO_VENCIDO_COMPROVANTE_ENDERECO")
        }

        // Critério 3: Outros documentos complementares vencidos
        val docComplementar = caseRequest.documents.find {
            it.category == DocumentCategory.COMPLEMENTAR && it.uploadState == UploadState.READY
        }
        if (docComplementar?.validUntil != null && docComplementar.validUntil!!.isBefore(referenceDate)) {
            reasonCodes.add("DOCUMENTO_VENCIDO_COMPLEMENTAR")
        }

        val decision = if (reasonCodes.isEmpty()) {
            ProcessingDecision.APROVADA
        } else {
            ProcessingDecision.REJEITADA
        }

        val result = ProcessingResult(
            caseRequest = caseRequest,
            runNumber = job.runNumber,
            decision = decision,
            reasonCodes = objectMapper.writeValueAsString(reasonCodes),
            rulesVersion = caseRequest.rulesVersion,
            evaluatedAt = LocalDateTime.now()
        )
        val savedResult = processingResultRepository.save(result)

        // Atualização da solicitação
        caseRequest.status = when (decision) {
            ProcessingDecision.APROVADA -> CaseStatus.APROVADA
            ProcessingDecision.REJEITADA -> CaseStatus.REJEITADA
            ProcessingDecision.FALHA_TECNICA -> CaseStatus.FALHA_TECNICA
        }
        caseRequest.updatedAt = LocalDateTime.now()
        caseRequestRepository.save(caseRequest)

        // Finalização do Job
        job.state = JobState.COMPLETED
        job.leaseUntil = null
        job.leaseToken = null
        job.lastError = null
        job.updatedAt = LocalDateTime.now()
        processingJobRepository.save(job)

        // Histórico
        val eventType = if (decision == ProcessingDecision.APROVADA) "ANALISE_APROVADA" else "ANALISE_REJEITADA"
        val details = if (decision == ProcessingDecision.APROVADA) {
            "Todos os critérios atendidos na versão ${caseRequest.rulesVersion}"
        } else {
            "Pendências identificadas: ${reasonCodes.joinToString(", ")}"
        }
        historyService.record(caseRequest, eventType, "SYSTEM", details)

        // Notificação ao solicitante
        val notifTitle = if (decision == ProcessingDecision.APROVADA) {
            "Solicitação ${caseRequest.protocol} Aprovada"
        } else {
            "Solicitação ${caseRequest.protocol} Rejeitada"
        }
        val notifMsg = if (decision == ProcessingDecision.APROVADA) {
            "Sua documentação foi conferida com sucesso e aprovada pelo motor de regras."
        } else {
            "Sua solicitação foi rejeitada pelos seguintes motivos: ${reasonCodes.joinToString(", ")}."
        }
        notificationService.notify(caseRequest, caseRequest.ownerSubject, notifTitle, notifMsg)

        logger.info("Conferência concluída para ${caseRequest.protocol}: Veredito $decision")
        return savedResult
    }

    private fun markTechnicalFailure(job: ProcessingJob, reason: String): ProcessingResult {
        val caseRequest = job.caseRequest
        val failedAt = LocalDateTime.now()
        job.attemptCount += 1
        val retryDelaySeconds = TechnicalRetryPolicy.retryDelaySeconds(job.attemptCount)
        job.state = if (retryDelaySeconds == null) JobState.FAILED else JobState.SCHEDULED
        job.availableAt = retryDelaySeconds?.let(failedAt::plusSeconds) ?: failedAt
        job.lastError = reason.take(2000)
        job.leaseUntil = null
        job.leaseToken = null
        job.updatedAt = failedAt
        processingJobRepository.save(job)

        caseRequest.status = if (retryDelaySeconds == null) CaseStatus.FALHA_TECNICA else CaseStatus.PROCESSANDO
        caseRequest.updatedAt = failedAt
        caseRequestRepository.save(caseRequest)

        val result = ProcessingResult(
            caseRequest = caseRequest,
            runNumber = job.runNumber,
            decision = ProcessingDecision.FALHA_TECNICA,
            reasonCodes = objectMapper.writeValueAsString(listOf("FALHA_INFRAESTRUTURA")),
            rulesVersion = caseRequest.rulesVersion,
            evaluatedAt = failedAt
        )
        val persistedResult = if (retryDelaySeconds == null) processingResultRepository.save(result) else result
        val historyDetails = retryDelaySeconds?.let {
            "Falha na tentativa ${job.attemptCount}; nova tentativa automática em $it segundos. Motivo: $reason"
        } ?: "Limite de tentativas automáticas atingido. Motivo: $reason"
        historyService.record(caseRequest, "FALHA_TECNICA_REGISTRADA", "SYSTEM", historyDetails)

        val notificationMessage = retryDelaySeconds?.let {
            "Ocorreu uma falha técnica. Uma nova tentativa automática foi agendada em $it segundos."
        } ?: "Ocorreu uma falha técnica após ${job.attemptCount} tentativas. Um administrador pode solicitar reprocessamento."
        notificationService.notify(
            caseRequest = caseRequest,
            recipientSubject = caseRequest.ownerSubject,
            title = "Instabilidade no processamento de ${caseRequest.protocol}",
            message = notificationMessage
        )

        return persistedResult
    }
}
