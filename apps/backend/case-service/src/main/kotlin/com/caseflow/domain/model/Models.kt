package com.caseflow.domain.model

import com.caseflow.domain.enums.CaseStatus
import com.caseflow.domain.enums.DocumentCategory
import com.caseflow.domain.enums.JobState
import com.caseflow.domain.enums.ProcessingDecision
import com.caseflow.domain.enums.UploadState
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class CaseRequest(
    val id: UUID = UUID.randomUUID(),
    var protocol: String = "",
    var ownerSubject: UUID = UUID.randomUUID(),
    var ownerEmail: String = "",
    var title: String = "",
    var description: String = "",
    var caseType: String = "ANALISE_DOCUMENTAL",
    var status: CaseStatus = CaseStatus.RASCUNHO,
    var version: Long = 1,
    var processingRun: Int = 0,
    var rulesVersion: String = "DOCUMENTAL_V1",
    var submittedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    val documents: MutableList<CaseDocument> = mutableListOf()
    val histories: MutableList<CaseHistory> = mutableListOf()
    val results: MutableList<ProcessingResult> = mutableListOf()
}

class CaseDocument(
    val id: UUID = UUID.randomUUID(),
    val caseRequest: CaseRequest,
    var category: DocumentCategory,
    var fileName: String = "",
    var fileSize: Long = 0,
    var contentType: String = "application/pdf",
    var storageKey: String = "",
    var sha256: String = "",
    var validUntil: LocalDate? = null,
    var uploadState: UploadState = UploadState.READY,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

class ProcessingJob(
    val id: UUID = UUID.randomUUID(),
    val caseRequest: CaseRequest,
    val runNumber: Int,
    var state: JobState = JobState.SCHEDULED,
    var attemptCount: Int = 0,
    var availableAt: LocalDateTime = LocalDateTime.now(),
    var leaseUntil: LocalDateTime? = null,
    var leaseToken: UUID? = null,
    var lastError: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

class ProcessingResult(
    val id: UUID = UUID.randomUUID(),
    val caseRequest: CaseRequest,
    val runNumber: Int,
    val decision: ProcessingDecision,
    val reasonCodes: String = "[]",
    val rulesVersion: String = "DOCUMENTAL_V1",
    val evaluatedAt: LocalDateTime = LocalDateTime.now()
)

class CaseHistory(
    val id: UUID = UUID.randomUUID(),
    val caseRequest: CaseRequest,
    val eventType: String,
    val actorSubject: String,
    val details: String? = null,
    val occurredAt: LocalDateTime = LocalDateTime.now()
)

class Notification(
    val id: UUID = UUID.randomUUID(),
    val caseRequest: CaseRequest,
    val recipientSubject: UUID,
    val title: String,
    val message: String,
    var readAt: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

class IdempotencyRecord(
    val id: UUID = UUID.randomUUID(),
    val operation: String,
    val idempotencyKey: String,
    val requestHash: String,
    val resourceId: UUID,
    val responseStatus: Int,
    val responseBody: String,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
