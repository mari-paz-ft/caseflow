package com.caseflow.bff.infrastructure.client.case

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class CaseServiceCaseWire(
    val id: UUID,
    val protocol: String,
    val ownerSubject: UUID,
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
    val documents: List<CaseServiceDocumentWire> = emptyList(),
    val latestResult: CaseServiceResultWire? = null
)

data class CaseServiceDocumentWire(
    val id: UUID,
    val category: String,
    val fileName: String,
    val fileSize: Long,
    val contentType: String,
    val validUntil: LocalDate?,
    val uploadState: String,
    val createdAt: LocalDateTime
)

data class CaseServiceResultWire(
    val id: UUID,
    val runNumber: Int,
    val decision: String,
    val reasonCodes: List<String>,
    val rulesVersion: String,
    val evaluatedAt: LocalDateTime
)

data class CaseServiceHistoryWire(
    val id: UUID,
    val caseId: UUID,
    val eventType: String,
    val actorSubject: String,
    val details: String?,
    val occurredAt: LocalDateTime
)

data class CaseServiceNotificationWire(
    val id: UUID,
    val caseId: UUID,
    val title: String,
    val message: String,
    val readAt: LocalDateTime?,
    val createdAt: LocalDateTime
)

data class CaseServiceCreateCaseWire(val title: String, val description: String, val type: String)
data class CaseServiceUpdateCaseWire(val title: String, val description: String, val version: Long)
data class CaseServiceSubmitCaseWire(val version: Long)
data class CaseServiceRetryCaseWire(val justification: String)

data class CaseServiceErrorWire(
    val errorCode: String? = null,
    val message: String? = null,
    val traceId: String? = null
)
