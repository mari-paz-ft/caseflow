package com.caseflow.bff.infrastructure.client.case

import com.caseflow.bff.application.dto.BffCaseHistoryDto
import com.caseflow.bff.application.dto.BffCaseResponseDto
import com.caseflow.bff.application.dto.BffCreateCaseRequest
import com.caseflow.bff.application.dto.BffNotificationDto
import com.caseflow.bff.application.dto.BffRetryCaseRequest
import com.caseflow.bff.application.dto.BffSubmitCaseRequest
import com.caseflow.bff.application.dto.BffUpdateCaseRequest
import com.caseflow.bff.application.port.CaseServiceClient
import com.caseflow.bff.application.port.DownloadedDocument
import com.caseflow.bff.application.port.DownstreamRequestHeaders
import com.caseflow.bff.application.port.UploadDocumentCommand
import com.caseflow.bff.application.port.UploadedDocument
import com.caseflow.bff.infrastructure.client.DownstreamCallExecutor
import com.caseflow.bff.infrastructure.client.copyRelayHeaders
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import java.util.UUID

@Component
class RestCaseServiceClient(
    @Qualifier("caseServiceRestClient") private val restClient: RestClient,
    private val downstreamCallExecutor: DownstreamCallExecutor
) : CaseServiceClient {
    override fun createCase(request: BffCreateCaseRequest, headers: DownstreamRequestHeaders): BffCaseResponseDto =
        downstreamCallExecutor.execute("case-service") {
            restClient.post()
                .uri("/api/v1/cases")
                .headers { it.copyRelayHeaders(headers) }
                .contentType(MediaType.APPLICATION_JSON)
                .body(CaseServiceCreateCaseWire(request.title, request.description, request.type))
                .retrieve()
                .body(CaseServiceCaseWire::class.java)
                ?.let(CaseServiceDtoMapper::toBffCase)
                ?: error("case-service retornou uma resposta vazia")
        }

    override fun listCases(headers: DownstreamRequestHeaders): List<BffCaseResponseDto> =
        downstreamCallExecutor.execute("case-service") {
            restClient.get()
                .uri("/api/v1/cases")
                .headers { it.copyRelayHeaders(headers) }
                .retrieve()
                .body(object : ParameterizedTypeReference<List<CaseServiceCaseWire>>() {})
                .orEmpty()
                .map(CaseServiceDtoMapper::toBffCase)
        }

    override fun getCase(id: UUID, headers: DownstreamRequestHeaders): BffCaseResponseDto =
        downstreamCallExecutor.execute("case-service") {
            restClient.get()
                .uri("/api/v1/cases/{id}", id)
                .headers { it.copyRelayHeaders(headers) }
                .retrieve()
                .body(CaseServiceCaseWire::class.java)
                ?.let(CaseServiceDtoMapper::toBffCase)
                ?: error("case-service retornou uma resposta vazia")
        }

    override fun updateCase(
        id: UUID,
        request: BffUpdateCaseRequest,
        headers: DownstreamRequestHeaders
    ): BffCaseResponseDto = downstreamCallExecutor.execute("case-service") {
        restClient.put()
            .uri("/api/v1/cases/{id}", id)
            .headers { it.copyRelayHeaders(headers) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(CaseServiceUpdateCaseWire(request.title, request.description, request.version))
            .retrieve()
            .body(CaseServiceCaseWire::class.java)
            ?.let(CaseServiceDtoMapper::toBffCase)
            ?: error("case-service retornou uma resposta vazia")
    }

    override fun uploadDocument(
        id: UUID,
        command: UploadDocumentCommand,
        headers: DownstreamRequestHeaders
    ): UploadedDocument = downstreamCallExecutor.execute("case-service") {
        val parts = LinkedMultiValueMap<String, Any>()
        parts.add("category", command.category)
        command.validUntil?.let { parts.add("validUntil", it) }
        val fileHeaders = HttpHeaders().apply {
            contentType = MediaType.parseMediaType(command.contentType)
            contentDisposition = org.springframework.http.ContentDisposition.formData()
                .name("file")
                .filename(command.fileName)
                .build()
        }
        val file = object : ByteArrayResource(command.content) {
            override fun getFilename(): String = command.fileName
        }
        parts.add("file", HttpEntity(file, fileHeaders))

        val response = restClient.post()
            .uri("/api/v1/cases/{id}/documents", id)
            .headers { it.copyRelayHeaders(headers) }
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(parts)
            .retrieve()
            .toEntity(CaseServiceDocumentWire::class.java)
        val document = response.body ?: error("case-service retornou uma resposta vazia")
        UploadedDocument(
            document = CaseServiceDtoMapper.toBffDocument(document),
            caseVersion = response.headers.getFirst("X-Case-Version")
        )
    }

    override fun deleteDocument(id: UUID, documentId: UUID, headers: DownstreamRequestHeaders): String? =
        downstreamCallExecutor.execute("case-service") {
            restClient.delete()
                .uri("/api/v1/cases/{id}/documents/{documentId}", id, documentId)
                .headers { it.copyRelayHeaders(headers) }
                .retrieve()
                .toBodilessEntity()
                .headers
                .getFirst("X-Case-Version")
        }

    override fun downloadDocument(id: UUID, documentId: UUID, headers: DownstreamRequestHeaders): DownloadedDocument =
        downstreamCallExecutor.execute("case-service") {
            val response = restClient.get()
                .uri("/api/v1/cases/{id}/documents/{documentId}/content", id, documentId)
                .headers { it.copyRelayHeaders(headers) }
                .retrieve()
                .toEntity(ByteArray::class.java)
            DownloadedDocument(
                content = response.body ?: ByteArray(0),
                contentType = response.headers.contentType?.toString() ?: MediaType.APPLICATION_PDF_VALUE,
                fileName = response.headers.contentDisposition.filename ?: "document-$documentId.pdf"
            )
        }

    override fun submitCase(
        id: UUID,
        request: BffSubmitCaseRequest,
        headers: DownstreamRequestHeaders
    ): BffCaseResponseDto = downstreamCallExecutor.execute("case-service") {
        restClient.post()
            .uri("/api/v1/cases/{id}/submit", id)
            .headers { it.copyRelayHeaders(headers) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(CaseServiceSubmitCaseWire(request.version))
            .retrieve()
            .body(CaseServiceCaseWire::class.java)
            ?.let(CaseServiceDtoMapper::toBffCase)
            ?: error("case-service retornou uma resposta vazia")
    }

    override fun getHistory(id: UUID, headers: DownstreamRequestHeaders): List<BffCaseHistoryDto> =
        downstreamCallExecutor.execute("case-service") {
            restClient.get()
                .uri("/api/v1/cases/{id}/history", id)
                .headers { it.copyRelayHeaders(headers) }
                .retrieve()
                .body(object : ParameterizedTypeReference<List<CaseServiceHistoryWire>>() {})
                .orEmpty()
                .map(CaseServiceDtoMapper::toBffHistory)
        }

    override fun retryCase(
        id: UUID,
        request: BffRetryCaseRequest,
        headers: DownstreamRequestHeaders
    ): BffCaseResponseDto = downstreamCallExecutor.execute("case-service") {
        restClient.post()
            .uri("/api/v1/cases/{id}/retry", id)
            .headers { it.copyRelayHeaders(headers) }
            .contentType(MediaType.APPLICATION_JSON)
            .body(CaseServiceRetryCaseWire(request.justification))
            .retrieve()
            .body(CaseServiceCaseWire::class.java)
            ?.let(CaseServiceDtoMapper::toBffCase)
            ?: error("case-service retornou uma resposta vazia")
    }

    override fun getNotifications(headers: DownstreamRequestHeaders): List<BffNotificationDto> =
        downstreamCallExecutor.execute("case-service") {
            restClient.get()
                .uri("/api/v1/notifications")
                .headers { it.copyRelayHeaders(headers) }
                .retrieve()
                .body(object : ParameterizedTypeReference<List<CaseServiceNotificationWire>>() {})
                .orEmpty()
                .map(CaseServiceDtoMapper::toBffNotification)
        }

    override fun markNotificationRead(id: UUID, headers: DownstreamRequestHeaders): BffNotificationDto =
        downstreamCallExecutor.execute("case-service") {
            restClient.patch()
                .uri("/api/v1/notifications/{id}", id)
                .headers { it.copyRelayHeaders(headers) }
                .retrieve()
                .body(CaseServiceNotificationWire::class.java)
                ?.let(CaseServiceDtoMapper::toBffNotification)
                ?: error("case-service retornou uma resposta vazia")
        }
}
