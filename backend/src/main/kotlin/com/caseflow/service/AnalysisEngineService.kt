package com.caseflow.service

import com.caseflow.domain.enums.*
import com.caseflow.domain.model.CaseRequest
import com.caseflow.domain.model.ProcessingJob
import com.caseflow.domain.model.ProcessingResult
import com.caseflow.repository.CaseRequestRepository
import com.caseflow.repository.ProcessingJobRepository
import com.caseflow.repository.ProcessingResultRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class AnalysisEngineService(
    private val caseRequestRepository: CaseRequestRepository,
    private val processingJobRepository: ProcessingJobRepository,
    private val processingResultRepository: ProcessingResultRepository,
    private val historyService: HistoryService,
    private val notificationService: NotificationService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(AnalysisEngineService::class.java)

    @Async
    @Transactional
    fun processJobAsync(job: ProcessingJob) {
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
        processingJobRepository.save(job)

        caseRequest.status = CaseStatus.PROCESSANDO
        caseRequestRepository.save(caseRequest)

        historyService.record(
            caseRequest = caseRequest,
            eventType = "PROCESSAMENTO_INICIADO",
            actorSubject = "SYSTEM",
            details = "Executor interno assumiu execução #${job.runNumber}"
        )

        // Verificação de simulação explícita de falha técnica
        if (caseRequest.title.contains("[SIMULAR_FALHA]", ignoreCase = true) ||
            caseRequest.description.contains("[SIMULAR_FALHA]", ignoreCase = true)
        ) {
            return markTechnicalFailure(job, "Simulação de indisponibilidade de I/O de armazenamento")
        }

        val reasonCodes = mutableListOf<String>()
        val referenceDate = caseRequest.submittedAt?.toLocalDate() ?: LocalDate.now()

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
        job.state = JobState.FAILED
        processingJobRepository.save(job)

        caseRequest.status = CaseStatus.FALHA_TECNICA
        caseRequest.updatedAt = LocalDateTime.now()
        caseRequestRepository.save(caseRequest)

        val result = ProcessingResult(
            caseRequest = caseRequest,
            runNumber = job.runNumber,
            decision = ProcessingDecision.FALHA_TECNICA,
            reasonCodes = objectMapper.writeValueAsString(listOf("FALHA_INFRAESTRUTURA")),
            rulesVersion = caseRequest.rulesVersion,
            evaluatedAt = LocalDateTime.now()
        )
        val savedResult = processingResultRepository.save(result)

        historyService.record(
            caseRequest = caseRequest,
            eventType = "FALHA_TECNICA_REGISTRADA",
            actorSubject = "SYSTEM",
            details = reason
        )

        notificationService.notify(
            caseRequest = caseRequest,
            recipientSubject = caseRequest.ownerSubject,
            title = "Instabilidade no processamento de ${caseRequest.protocol}",
            message = "Ocorreu uma falha técnica durante a conferência. Um administrador foi notificado para reprocessamento."
        )

        return savedResult
    }
}
