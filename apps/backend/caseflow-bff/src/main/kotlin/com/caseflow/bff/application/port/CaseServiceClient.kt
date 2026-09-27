package com.caseflow.bff.application.port

import com.caseflow.bff.application.dto.BffCaseDocumentDto
import com.caseflow.bff.application.dto.BffCaseHistoryDto
import com.caseflow.bff.application.dto.BffCaseResponseDto
import com.caseflow.bff.application.dto.BffCreateCaseRequest
import com.caseflow.bff.application.dto.BffNotificationDto
import com.caseflow.bff.application.dto.BffRetryCaseRequest
import com.caseflow.bff.application.dto.BffSubmitCaseRequest
import com.caseflow.bff.application.dto.BffUpdateCaseRequest
import java.util.UUID

data class DownstreamRequestHeaders(
    val authorization: String,
    val idempotencyKey: String? = null,
    val correlationId: String? = null
)

data class UploadDocumentCommand(
    val category: String,
    val validUntil: String?,
    val fileName: String,
    val contentType: String,
    val content: ByteArray
)

data class UploadedDocument(
    val document: BffCaseDocumentDto,
    val caseVersion: String?
)

data class DownloadedDocument(
    val content: ByteArray,
    val contentType: String,
    val fileName: String
)

interface CaseServiceClient {
    fun createCase(request: BffCreateCaseRequest, headers: DownstreamRequestHeaders): BffCaseResponseDto
    fun listCases(headers: DownstreamRequestHeaders): List<BffCaseResponseDto>
    fun getCase(id: UUID, headers: DownstreamRequestHeaders): BffCaseResponseDto
    fun updateCase(id: UUID, request: BffUpdateCaseRequest, headers: DownstreamRequestHeaders): BffCaseResponseDto
    fun uploadDocument(id: UUID, command: UploadDocumentCommand, headers: DownstreamRequestHeaders): UploadedDocument
    fun deleteDocument(id: UUID, documentId: UUID, headers: DownstreamRequestHeaders): String?
    fun downloadDocument(id: UUID, documentId: UUID, headers: DownstreamRequestHeaders): DownloadedDocument
    fun submitCase(id: UUID, request: BffSubmitCaseRequest, headers: DownstreamRequestHeaders): BffCaseResponseDto
    fun getHistory(id: UUID, headers: DownstreamRequestHeaders): List<BffCaseHistoryDto>
    fun retryCase(id: UUID, request: BffRetryCaseRequest, headers: DownstreamRequestHeaders): BffCaseResponseDto
    fun getNotifications(headers: DownstreamRequestHeaders): List<BffNotificationDto>
    fun markNotificationRead(id: UUID, headers: DownstreamRequestHeaders): BffNotificationDto
}
