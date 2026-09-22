package com.caseflow.service

import com.caseflow.controller.dto.*
import com.caseflow.domain.enums.*
import com.caseflow.domain.exception.*
import com.caseflow.domain.model.*
import com.caseflow.repository.CaseRequestRepository
import com.caseflow.repository.ProcessingJobRepository
import com.caseflow.repository.ProcessingResultRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.random.Random

@Service
class CaseService(
    private val caseRequestRepository: CaseRequestRepository,
    private val processingJobRepository: ProcessingJobRepository,
    private val processingResultRepository: ProcessingResultRepository,
    private val analysisEngineService: AnalysisEngineService,
    private val historyService: HistoryService,
    private val objectMapper: ObjectMapper
) {
    @Transactional
    fun createCase(dto: CreateCaseDto, currentUser: AppUser): CaseResponseDto {
        val protocol = generateProtocol()
        val caseRequest = CaseRequest(
            protocol = protocol,
            ownerSubject = currentUser.id,
            ownerEmail = currentUser.email,
            title = dto.title.trim(),
            description = dto.description.trim(),
            caseType = dto.type,
            status = CaseStatus.RASCUNHO,
            version = 1,
            processingRun = 0,
            rulesVersion = "DOCUMENTAL_V1"
        )
        val saved = caseRequestRepository.save(caseRequest)
        historyService.record(
            caseRequest = saved,
            eventType = "CRIACAO_RASCUNHO",
            actorSubject = currentUser.email,
            details = "Rascunho criado com protocolo $protocol"
        )
        return toDto(saved)
    }

    @Transactional(readOnly = true)
    fun listCases(currentUser: AppUser): List<CaseResponseDto> {
        val cases = if (currentUser.role == RoleName.ROLE_ADMIN) {
            caseRequestRepository.findAllByOrderByCreatedAtDesc()
        } else {
            caseRequestRepository.findByOwnerSubjectOrderByCreatedAtDesc(currentUser.id)
        }
        return cases.map { toDto(it) }
    }

    @Transactional(readOnly = true)
    fun getCaseById(id: UUID, currentUser: AppUser): CaseResponseDto {
        val caseRequest = caseRequestRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Solicitação não encontrada: $id") }

        if (currentUser.role != RoleName.ROLE_ADMIN && caseRequest.ownerSubject != currentUser.id) {
            throw ForbiddenException("Você não tem permissão para acessar esta solicitação")
        }
        return toDto(caseRequest)
    }

    @Transactional
    fun updateCase(id: UUID, dto: UpdateCaseDto, currentUser: AppUser): CaseResponseDto {
        val caseRequest = caseRequestRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Solicitação não encontrada: $id") }

        if (caseRequest.ownerSubject != currentUser.id) {
            throw ForbiddenException("Apenas o autor pode editar o rascunho")
        }
        if (caseRequest.status != CaseStatus.RASCUNHO) {
            throw ConflictException("Solicitações em estado ${caseRequest.status} são imutáveis", "CASE_IMMUTABLE")
        }
        if (caseRequest.version != dto.version) {
            throw ConflictException("Conflito de versão (esperado: ${caseRequest.version}, recebido: ${dto.version})", "VERSION_MISMATCH")
        }

        caseRequest.title = dto.title.trim()
        caseRequest.description = dto.description.trim()
        caseRequest.updatedAt = LocalDateTime.now()
        caseRequest.version += 1

        val updated = caseRequestRepository.save(caseRequest)
        historyService.record(
            caseRequest = updated,
            eventType = "RASCUNHO_ATUALIZADO",
            actorSubject = currentUser.email,
            details = "Título e descrição atualizados"
        )
        return toDto(updated)
    }

    @Transactional
    fun submitCase(id: UUID, dto: SubmitCaseDto, currentUser: AppUser, idempotencyKey: String?): CaseResponseDto {
        val caseRequest = caseRequestRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Solicitação não encontrada: $id") }

        if (caseRequest.ownerSubject != currentUser.id) {
            throw ForbiddenException("Apenas o autor pode enviar a solicitação")
        }
        if (caseRequest.status != CaseStatus.RASCUNHO) {
            if (caseRequest.status == CaseStatus.ENVIADA || caseRequest.status == CaseStatus.PROCESSANDO || caseRequest.status == CaseStatus.APROVADA || caseRequest.status == CaseStatus.REJEITADA) {
                // Suporte a Idempotência: caso já enviada anteriormente
                return toDto(caseRequest)
            }
            throw ConflictException("A solicitação não está no estado RASCUNHO", "INVALID_STATE_FOR_SUBMIT")
        }
        if (caseRequest.version != dto.version) {
            throw ConflictException("Conflito de versão (esperado: ${caseRequest.version}, recebido: ${dto.version})", "VERSION_MISMATCH")
        }
        if (caseRequest.documents.isEmpty()) {
            throw ConflictException("Envio exige pelo menos um documento anexado", "NO_DOCUMENTS_ATTACHED")
        }

        caseRequest.status = CaseStatus.ENVIADA
        caseRequest.submittedAt = LocalDateTime.now()
        caseRequest.updatedAt = LocalDateTime.now()
        caseRequest.processingRun += 1
        caseRequest.version += 1

        val savedCase = caseRequestRepository.save(caseRequest)

        val job = ProcessingJob(
            caseRequest = savedCase,
            runNumber = savedCase.processingRun,
            state = JobState.SCHEDULED
        )
        val savedJob = processingJobRepository.save(job)

        historyService.record(
            caseRequest = savedCase,
            eventType = "SOLICITACAO_ENVIADA",
            actorSubject = currentUser.email,
            details = "Solicitação enviada para conferência documental (Run #${savedCase.processingRun})"
        )

        // Aciona o motor assíncrono interno
        analysisEngineService.processJobAsync(savedJob)

        return toDto(savedCase)
    }

    @Transactional
    fun retryCase(id: UUID, dto: RetryCaseDto, currentUser: AppUser, idempotencyKey: String?): CaseResponseDto {
        if (currentUser.role != RoleName.ROLE_ADMIN) {
            throw ForbiddenException("Apenas administradores podem solicitar reprocessamento")
        }
        val caseRequest = caseRequestRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Solicitação não encontrada: $id") }

        if (caseRequest.status != CaseStatus.FALHA_TECNICA) {
            throw ConflictException("Reprocessamento administrativo só é permitido em FALHA_TECNICA", "INVALID_STATE_FOR_RETRY")
        }

        caseRequest.status = CaseStatus.ENVIADA
        caseRequest.updatedAt = LocalDateTime.now()
        caseRequest.processingRun += 1
        caseRequest.version += 1

        val savedCase = caseRequestRepository.save(caseRequest)

        val job = ProcessingJob(
            caseRequest = savedCase,
            runNumber = savedCase.processingRun,
            state = JobState.SCHEDULED
        )
        val savedJob = processingJobRepository.save(job)

        historyService.record(
            caseRequest = savedCase,
            eventType = "REPROCESSAMENTO_SOLICITADO",
            actorSubject = currentUser.email,
            details = "Justificativa: ${dto.justification}"
        )

        analysisEngineService.processJobAsync(savedJob)

        return toDto(savedCase)
    }

    private fun generateProtocol(): String {
        val dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        val randomDigits = Random.nextInt(1000, 9999)
        return "CF-$dateStr-$randomDigits"
    }

    fun toDto(caseRequest: CaseRequest): CaseResponseDto {
        val latestResult = processingResultRepository.findFirstByCaseRequestIdOrderByRunNumberDesc(caseRequest.id)
            .map { res ->
                val reasonList: List<String> = try {
                    objectMapper.readValue(res.reasonCodes, object : TypeReference<List<String>>() {})
                } catch (e: Exception) {
                    emptyList()
                }
                ProcessingResultDto(
                    id = res.id,
                    runNumber = res.runNumber,
                    decision = res.decision,
                    reasonCodes = reasonList,
                    rulesVersion = res.rulesVersion,
                    evaluatedAt = res.evaluatedAt
                )
            }.orElse(null)

        val docDtos = caseRequest.documents.map { doc ->
            CaseDocumentDto(
                id = doc.id,
                category = doc.category,
                fileName = doc.fileName,
                fileSize = doc.fileSize,
                contentType = doc.contentType,
                validUntil = doc.validUntil,
                uploadState = doc.uploadState,
                createdAt = doc.createdAt
            )
        }

        return CaseResponseDto(
            id = caseRequest.id,
            protocol = caseRequest.protocol,
            ownerSubject = caseRequest.ownerSubject,
            ownerEmail = caseRequest.ownerEmail,
            title = caseRequest.title,
            description = caseRequest.description,
            caseType = caseRequest.caseType,
            status = caseRequest.status,
            version = caseRequest.version,
            processingRun = caseRequest.processingRun,
            rulesVersion = caseRequest.rulesVersion,
            submittedAt = caseRequest.submittedAt,
            createdAt = caseRequest.createdAt,
            updatedAt = caseRequest.updatedAt,
            documents = docDtos,
            latestResult = latestResult
        )
    }
}
