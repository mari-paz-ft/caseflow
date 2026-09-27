package com.caseflow.bff.interfaces.rest

import com.caseflow.bff.application.dto.BffCaseHistoryDto
import com.caseflow.bff.application.dto.BffCaseResponseDto
import com.caseflow.bff.application.dto.BffCreateCaseRequest
import com.caseflow.bff.application.dto.BffNotificationDto
import com.caseflow.bff.application.dto.BffRetryCaseRequest
import com.caseflow.bff.application.dto.BffSubmitCaseRequest
import com.caseflow.bff.application.dto.BffUpdateCaseRequest
import com.caseflow.bff.application.port.CaseServiceClient
import com.caseflow.bff.application.port.DownstreamRequestHeaders
import com.caseflow.bff.application.port.UploadDocumentCommand
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/bff/v1")
class CaseController(
    private val caseServiceClient: CaseServiceClient
) {
    @PostMapping("/cases")
    fun createCase(
        @Valid @RequestBody request: BffCreateCaseRequest,
        @RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String,
        @RequestHeader(value = "Correlation-Id", required = false) correlationId: String?
    ): ResponseEntity<BffCaseResponseDto> {
        val response = caseServiceClient.createCase(request, headers(authorization, correlationId = correlationId))
        return ResponseEntity.created(URI.create("/bff/v1/cases/${response.id}")).body(response)
    }

    @GetMapping("/cases")
    fun listCases(
        @RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String,
        @RequestHeader(value = "Correlation-Id", required = false) correlationId: String?
    ): ResponseEntity<List<BffCaseResponseDto>> =
        ResponseEntity.ok(caseServiceClient.listCases(headers(authorization, correlationId = correlationId)))

    @GetMapping("/cases/{id}")
    fun getCase(
        @PathVariable id: UUID,
        @RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String,
        @RequestHeader(value = "Correlation-Id", required = false) correlationId: String?
    ): ResponseEntity<BffCaseResponseDto> =
        ResponseEntity.ok(caseServiceClient.getCase(id, headers(authorization, correlationId = correlationId)))

    @PutMapping("/cases/{id}")
    fun updateCase(
        @PathVariable id: UUID,
        @Valid @RequestBody request: BffUpdateCaseRequest,
        @RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String,
        @RequestHeader(value = "Correlation-Id", required = false) correlationId: String?
    ): ResponseEntity<BffCaseResponseDto> =
        ResponseEntity.ok(caseServiceClient.updateCase(id, request, headers(authorization, correlationId = correlationId)))

    @PostMapping("/cases/{id}/documents")
    fun uploadDocument(
        @PathVariable id: UUID,
        @RequestParam("category") category: String,
        @RequestParam(value = "validUntil", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) validUntil: LocalDate?,
        @RequestParam("file") file: MultipartFile,
        @RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String,
        @RequestHeader(value = "Correlation-Id", required = false) correlationId: String?
    ): ResponseEntity<com.caseflow.bff.application.dto.BffCaseDocumentDto> {
        val uploaded = caseServiceClient.uploadDocument(
            id,
            UploadDocumentCommand(
                category = category,
                validUntil = validUntil?.toString(),
                fileName = file.originalFilename ?: "document.pdf",
                contentType = file.contentType ?: MediaType.APPLICATION_OCTET_STREAM_VALUE,
                content = file.bytes
            ),
            headers(authorization, correlationId = correlationId)
        )
        val response = ResponseEntity.status(HttpStatus.CREATED)
        uploaded.caseVersion?.let { response.header("X-Case-Version", it) }
        return response.body(uploaded.document)
    }

    @DeleteMapping("/cases/{id}/documents/{documentId}")
    fun deleteDocument(
        @PathVariable id: UUID,
        @PathVariable documentId: UUID,
        @RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String,
        @RequestHeader(value = "Correlation-Id", required = false) correlationId: String?
    ): ResponseEntity<Void> {
        val version = caseServiceClient.deleteDocument(id, documentId, headers(authorization, correlationId = correlationId))
        val response = ResponseEntity.noContent()
        version?.let { response.header("X-Case-Version", it) }
        return response.build()
    }

    @GetMapping("/cases/{id}/documents/{documentId}/content")
    fun downloadDocument(
        @PathVariable id: UUID,
        @PathVariable documentId: UUID,
        @RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String,
        @RequestHeader(value = "Correlation-Id", required = false) correlationId: String?
    ): ResponseEntity<ByteArray> {
        val document = caseServiceClient.downloadDocument(id, documentId, headers(authorization, correlationId = correlationId))
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(document.contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${document.fileName}\"")
            .body(document.content)
    }

    @PostMapping("/cases/{id}/submit")
    fun submitCase(
        @PathVariable id: UUID,
        @Valid @RequestBody request: BffSubmitCaseRequest,
        @RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String,
        @RequestHeader(value = "Idempotency-Key") idempotencyKey: String,
        @RequestHeader(value = "Correlation-Id", required = false) correlationId: String?
    ): ResponseEntity<BffCaseResponseDto> = ResponseEntity.status(HttpStatus.ACCEPTED).body(
        caseServiceClient.submitCase(id, request, headers(authorization, idempotencyKey, correlationId))
    )

    @GetMapping("/cases/{id}/history")
    fun getHistory(
        @PathVariable id: UUID,
        @RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String,
        @RequestHeader(value = "Correlation-Id", required = false) correlationId: String?
    ): ResponseEntity<List<BffCaseHistoryDto>> =
        ResponseEntity.ok(caseServiceClient.getHistory(id, headers(authorization, correlationId = correlationId)))

    @PostMapping("/cases/{id}/retry")
    fun retryCase(
        @PathVariable id: UUID,
        @Valid @RequestBody request: BffRetryCaseRequest,
        @RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String,
        @RequestHeader(value = "Idempotency-Key") idempotencyKey: String,
        @RequestHeader(value = "Correlation-Id", required = false) correlationId: String?
    ): ResponseEntity<BffCaseResponseDto> = ResponseEntity.status(HttpStatus.ACCEPTED).body(
        caseServiceClient.retryCase(id, request, headers(authorization, idempotencyKey, correlationId))
    )

    @GetMapping("/notifications")
    fun getNotifications(
        @RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String,
        @RequestHeader(value = "Correlation-Id", required = false) correlationId: String?
    ): ResponseEntity<List<BffNotificationDto>> =
        ResponseEntity.ok(caseServiceClient.getNotifications(headers(authorization, correlationId = correlationId)))

    @PatchMapping("/notifications/{id}")
    fun markNotificationRead(
        @PathVariable id: UUID,
        @RequestHeader(HttpHeaders.AUTHORIZATION) authorization: String,
        @RequestHeader(value = "Correlation-Id", required = false) correlationId: String?
    ): ResponseEntity<BffNotificationDto> =
        ResponseEntity.ok(caseServiceClient.markNotificationRead(id, headers(authorization, correlationId = correlationId)))

    private fun headers(
        authorization: String,
        idempotencyKey: String? = null,
        correlationId: String? = null
    ) = DownstreamRequestHeaders(authorization, idempotencyKey, correlationId)
}
