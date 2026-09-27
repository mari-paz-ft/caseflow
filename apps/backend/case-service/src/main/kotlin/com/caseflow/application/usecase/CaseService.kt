package com.caseflow.application.usecase

import com.caseflow.application.dto.*
import com.caseflow.domain.enums.*
import com.caseflow.domain.exception.*
import com.caseflow.domain.model.*
import com.caseflow.domain.rule.CaseAuthorizationPolicy
import com.caseflow.application.port.CaseRequestRepository
import com.caseflow.application.port.IdempotencyRecordRepository
import com.caseflow.application.port.ProcessingJobRepository
import com.caseflow.application.port.ProcessingResultRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.security.MessageDigest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.random.Random

@Service
class CaseService(
    private val caseRequestRepository: CaseRequestRepository,
    private val processingJobRepository: ProcessingJobRepository,
    private val processingResultRepository: ProcessingResultRepository,
    private val idempotencyRecordRepository: IdempotencyRecordRepository,
    private val historyService: HistoryService,
    private val objectMapper: ObjectMapper
) {
    @Transactional
    fun createCase(dto: CreateCaseDto, currentUser: CaseActor): CaseResponseDto {
        val protocol = generateProtocol()
        val caseRequest = CaseRequest(
            protocol = protocol,
            ownerSubject = currentUser.subject,
            ownerEmail = currentUser.username,
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
            actorSubject = currentUser.subject.toString(),
            details = "Rascunho criado com protocolo $protocol"
        )
        return toDto(saved)
    }

    @Transactional(readOnly = true)
    fun listCases(currentUser: CaseActor): List<CaseResponseDto> {
        val cases = if (currentUser.isAdmin) {
            caseRequestRepository.findAllByOrderByCreatedAtDesc()
        } else {
            caseRequestRepository.findByOwnerSubjectOrderByCreatedAtDesc(currentUser.subject)
        }
        return cases.map { toDto(it) }
    }

    @Transactional(readOnly = true)
    fun getCaseById(id: UUID, currentUser: CaseActor): CaseResponseDto {
        val caseRequest = caseRequestRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Solicitação não encontrada: $id") }

        CaseAuthorizationPolicy.requireReadAccess(caseRequest, currentUser)
        return toDto(caseRequest)
    }

    @Transactional
    fun updateCase(id: UUID, dto: UpdateCaseDto, currentUser: CaseActor): CaseResponseDto {
        val caseRequest = caseRequestRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Solicitação não encontrada: $id") }

        CaseAuthorizationPolicy.requireOwner(caseRequest, currentUser)
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
            actorSubject = currentUser.subject.toString(),
            details = "Título e descrição atualizados"
        )
        return toDto(updated)
    }

    @Transactional
    fun submitCase(
        id: UUID,
        dto: SubmitCaseDto,
        currentUser: CaseActor,
        idempotencyKey: String
    ): IdempotentResult<CaseResponseDto> {
        val key = requireIdempotencyKey(idempotencyKey)
        val caseRequest = caseRequestRepository.findByIdForUpdate(id)
            .orElseThrow { ResourceNotFoundException("Solicitação não encontrada: $id") }

        CaseAuthorizationPolicy.requireOwner(caseRequest, currentUser)
        val requestHash = requestHash("SUBMIT", currentUser, id, dto)
        // Suporte a Idempotência: caso já enviada anteriormente
        replayIdempotentResponse("SUBMIT", key, requestHash)?.let { return it }

        if (caseRequest.status != CaseStatus.RASCUNHO) {
            throw ConflictException("A solicitação não está no estado RASCUNHO", "INVALID_STATE_FOR_SUBMIT")
        }
        if (caseRequest.version != dto.version) {
            throw ConflictException("Conflito de versão (esperado: ${caseRequest.version}, recebido: ${dto.version})", "VERSION_MISMATCH")
        }
        if (caseRequest.documents.isEmpty()) {
            throw ConflictException("Envio exige pelo menos um documento anexado", "NO_DOCUMENTS_ATTACHED")
        }
        if (caseRequest.documents.none { it.uploadState == UploadState.READY }) {
            throw ConflictException("Envio exige pelo menos um documento anexado e finalizado", "NO_READY_DOCUMENTS")
        }

        caseRequest.status = CaseStatus.ENVIADA
        caseRequest.submittedAt = LocalDateTime.now(ZoneOffset.UTC)
        caseRequest.updatedAt = LocalDateTime.now()
        caseRequest.processingRun += 1
        caseRequest.version += 1

        val savedCase = caseRequestRepository.save(caseRequest)
        processingJobRepository.save(
            ProcessingJob(
                caseRequest = savedCase,
                runNumber = savedCase.processingRun,
                state = JobState.SCHEDULED
            )
        )

        historyService.record(
            caseRequest = savedCase,
            eventType = "SOLICITACAO_ENVIADA",
            actorSubject = currentUser.subject.toString(),
            details = "Solicitação enviada para conferência documental (Run #${savedCase.processingRun})"
        )

        val response = toDto(savedCase)
        val result = storeIdempotentResponse("SUBMIT", key, requestHash, id, response)
        return result
    }

    @Transactional
    fun retryCase(
        id: UUID,
        dto: RetryCaseDto,
        currentUser: CaseActor,
        idempotencyKey: String
    ): IdempotentResult<CaseResponseDto> {
        val key = requireIdempotencyKey(idempotencyKey)
        CaseAuthorizationPolicy.requireAdmin(currentUser)
        val caseRequest = caseRequestRepository.findByIdForUpdate(id)
            .orElseThrow { ResourceNotFoundException("Solicitação não encontrada: $id") }
        val requestHash = requestHash("RETRY", currentUser, id, dto)
        replayIdempotentResponse("RETRY", key, requestHash)?.let { return it }

        if (caseRequest.status != CaseStatus.FALHA_TECNICA) {
            throw ConflictException("Reprocessamento administrativo só é permitido em FALHA_TECNICA", "INVALID_STATE_FOR_RETRY")
        }

        caseRequest.status = CaseStatus.ENVIADA
        caseRequest.updatedAt = LocalDateTime.now()
        caseRequest.processingRun += 1
        caseRequest.version += 1

        val savedCase = caseRequestRepository.save(caseRequest)
        processingJobRepository.save(
            ProcessingJob(
                caseRequest = savedCase,
                runNumber = savedCase.processingRun,
                state = JobState.SCHEDULED
            )
        )

        historyService.record(
            caseRequest = savedCase,
            eventType = "REPROCESSAMENTO_SOLICITADO",
            actorSubject = currentUser.subject.toString(),
            details = "Justificativa: ${dto.justification}"
        )

        val response = toDto(savedCase)
        val result = storeIdempotentResponse("RETRY", key, requestHash, id, response)
        return result
    }

    private fun requireIdempotencyKey(key: String): String =
        key.takeIf(String::isNotBlank)
            ?: throw CaseFlowException("Idempotency-Key é obrigatória", errorCode = "IDEMPOTENCY_KEY_REQUIRED")

    private fun requestHash(operation: String, actor: CaseActor, resourceId: UUID, request: Any): String {
        val canonicalContext = objectMapper.writeValueAsBytes(
            listOf(operation, actor.subject.toString(), resourceId.toString(), request)
        )
        return MessageDigest.getInstance("SHA-256")
            .digest(canonicalContext)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    private fun replayIdempotentResponse(
        operation: String,
        key: String,
        requestHash: String
    ): IdempotentResult<CaseResponseDto>? {
        val record = idempotencyRecordRepository.findByOperationAndIdempotencyKey(operation, key).orElse(null)
            ?: return null
        if (record.requestHash != requestHash) {
            throw ConflictException("Idempotency-Key já foi utilizada com outro contexto", "IDEMPOTENCY_KEY_REUSED")
        }
        val response = objectMapper.readValue(record.responseBody, CaseResponseDto::class.java)
        return IdempotentResult(record.responseStatus, response)
    }

    private fun storeIdempotentResponse(
        operation: String,
        key: String,
        requestHash: String,
        resourceId: UUID,
        response: CaseResponseDto
    ): IdempotentResult<CaseResponseDto> {
        val status = 202
        idempotencyRecordRepository.saveAndFlush(
            IdempotencyRecord(
                operation = operation,
                idempotencyKey = key,
                requestHash = requestHash,
                resourceId = resourceId,
                responseStatus = status,
                responseBody = objectMapper.writeValueAsString(response)
            )
        )
        return IdempotentResult(status, response)
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
