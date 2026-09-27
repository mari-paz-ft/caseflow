package com.caseflow.bff.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime


data class BffLoginRequest(
    @field:NotBlank val username: String,
    @field:NotBlank val password: String
)

data class BffUserDto(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String
)

data class BffLoginResponse(
    val token: String,
    val tokenType: String,
    val expiresAt: Instant,
    val user: BffUserDto
)

data class BffCreateCaseRequest(
    @field:NotBlank @field:Size(min = 5, max = 120) val title: String,
    @field:NotBlank @field:Size(min = 20, max = 2000) val description: String,
    @field:Pattern(regexp = "ANALISE_DOCUMENTAL", message = "Tipo de solicitação inválido")
    val type: String = "ANALISE_DOCUMENTAL"
)

data class BffUpdateCaseRequest(
    @field:NotBlank @field:Size(min = 5, max = 120) val title: String,
    @field:NotBlank @field:Size(min = 20, max = 2000) val description: String,
    val version: Long
)

data class BffSubmitCaseRequest(val version: Long)

data class BffRetryCaseRequest(
    @field:NotBlank @field:Size(min = 10, max = 500) val justification: String
)

data class BffCaseDocumentDto(
    val id: String,
    val category: String,
    val fileName: String,
    val fileSize: Long,
    val contentType: String,
    val validUntil: LocalDate?,
    val uploadState: String,
    val createdAt: LocalDateTime
)

data class BffProcessingResultDto(
    val id: String,
    val runNumber: Int,
    val decision: String,
    val reasonCodes: List<String>,
    val rulesVersion: String,
    val evaluatedAt: LocalDateTime
)

data class BffCaseResponseDto(
    val id: String,
    val protocol: String,
    val ownerSubject: String,
    val ownerEmail: String,
    val title: String,
    val description: String,
    val caseType: String,
    val status: String,
    val version: Long,
    val processingRun: Int,
    val rulesVersion: String,
    val submittedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val documents: List<BffCaseDocumentDto>,
    val latestResult: BffProcessingResultDto?
)

data class BffCaseHistoryDto(
    val id: String,
    val caseId: String,
    val eventType: String,
    val actorSubject: String,
    val details: String?,
    val occurredAt: LocalDateTime
)

data class BffNotificationDto(
    val id: String,
    val caseId: String,
    val title: String,
    val message: String,
    val readAt: LocalDateTime?,
    val createdAt: LocalDateTime
)

data class BffErrorResponse(
    val status: Int,
    val errorCode: String,
    val message: String,
    val traceId: String
)
