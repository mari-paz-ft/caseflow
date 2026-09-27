package com.caseflow.application.dto

import com.caseflow.domain.enums.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class CreateCaseDto(
    @field:NotBlank(message = "Título é obrigatório")
    @field:Size(min = 5, max = 120, message = "Título deve conter entre 5 e 120 caracteres")
    val title: String,

    @field:NotBlank(message = "Descrição é obrigatória")
    @field:Size(min = 20, max = 2000, message = "Descrição deve conter entre 20 e 2000 caracteres")
    val description: String,

    @field:Pattern(regexp = "ANALISE_DOCUMENTAL", message = "Tipo de solicitação inválido")
    val type: String = "ANALISE_DOCUMENTAL"
)

data class UpdateCaseDto(
    @field:NotBlank(message = "Título é obrigatório")
    @field:Size(min = 5, max = 120, message = "Título deve conter entre 5 e 120 caracteres")
    val title: String,

    @field:NotBlank(message = "Descrição é obrigatória")
    @field:Size(min = 20, max = 2000, message = "Descrição deve conter entre 20 e 2000 caracteres")
    val description: String,

    val version: Long
)

data class SubmitCaseDto(
    val version: Long
)

data class IdempotentResult<T>(
    val responseStatus: Int,
    val responseBody: T
)

data class UploadDocumentCommand(
    val category: DocumentCategory,
    val validUntil: LocalDate?,
    val contentType: String?,
    val originalFileName: String?,
    val content: ByteArray
)

data class RetryCaseDto(
    @field:NotBlank(message = "Justificativa é obrigatória")
    @field:Size(min = 10, max = 500, message = "Justificativa deve conter entre 10 e 500 caracteres")
    val justification: String
)

data class CaseDocumentDto(
    val id: UUID,
    val category: DocumentCategory,
    val fileName: String,
    val fileSize: Long,
    val contentType: String,
    val validUntil: LocalDate?,
    val uploadState: UploadState,
    val createdAt: LocalDateTime
)

data class ProcessingResultDto(
    val id: UUID,
    val runNumber: Int,
    val decision: ProcessingDecision,
    val reasonCodes: List<String>,
    val rulesVersion: String,
    val evaluatedAt: LocalDateTime
)

data class CaseResponseDto(
    val id: UUID,
    val protocol: String,
    val ownerSubject: UUID,
    val ownerEmail: String,
    val title: String,
    val description: String,
    val caseType: String,
    val status: CaseStatus,
    val version: Long,
    val processingRun: Int,
    val rulesVersion: String,
    val submittedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val documents: List<CaseDocumentDto> = emptyList(),
    val latestResult: ProcessingResultDto? = null
)

data class CaseHistoryDto(
    val id: UUID,
    val caseId: UUID,
    val eventType: String,
    val actorSubject: String,
    val details: String?,
    val occurredAt: LocalDateTime
)

data class NotificationDto(
    val id: UUID,
    val caseId: UUID,
    val title: String,
    val message: String,
    val readAt: LocalDateTime?,
    val createdAt: LocalDateTime
)
