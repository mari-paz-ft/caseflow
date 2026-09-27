package com.caseflow.infrastructure.persistence

import com.caseflow.domain.enums.CaseStatus
import com.caseflow.domain.enums.DocumentCategory
import com.caseflow.domain.enums.JobState
import com.caseflow.domain.enums.ProcessingDecision
import com.caseflow.domain.enums.UploadState
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "case_requests")
class CaseRequestEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true)
    var protocol: String = "",
    @Column(nullable = false)
    var ownerSubject: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    var ownerEmail: String = "",
    @Column(nullable = false, length = 120)
    var title: String = "",
    @Column(nullable = false, length = 2000)
    var description: String = "",
    @Column(nullable = false)
    var caseType: String = "ANALISE_DOCUMENTAL",
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CaseStatus = CaseStatus.RASCUNHO,
    @Version
    @Column(nullable = false)
    var version: Long = 1,
    @Column(nullable = false)
    var processingRun: Int = 0,
    @Column(nullable = false)
    var rulesVersion: String = "DOCUMENTAL_V1",
    var submittedAt: LocalDateTime? = null,
    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @OneToMany(mappedBy = "caseRequest", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var documents: MutableList<CaseDocumentEntity> = mutableListOf()

    @OneToMany(mappedBy = "caseRequest", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var histories: MutableList<CaseHistoryEntity> = mutableListOf()

    @OneToMany(mappedBy = "caseRequest", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var results: MutableList<ProcessingResultEntity> = mutableListOf()
}

@Entity
@Table(name = "case_documents")
class CaseDocumentEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    var caseRequest: CaseRequestEntity,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: DocumentCategory,
    @Column(nullable = false)
    var fileName: String = "",
    @Column(nullable = false)
    var fileSize: Long = 0,
    @Column(nullable = false)
    var contentType: String = "application/pdf",
    @Column(nullable = false, unique = true)
    var storageKey: String = "",
    @Column(nullable = false)
    var sha256: String = "",
    var validUntil: LocalDate? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var uploadState: UploadState = UploadState.READY,
    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "processing_jobs")
class ProcessingJobEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    var caseRequest: CaseRequestEntity,
    @Column(nullable = false)
    var runNumber: Int,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var state: JobState = JobState.SCHEDULED,
    @Column(nullable = false)
    var attemptCount: Int = 0,
    @Column(nullable = false)
    var availableAt: LocalDateTime = LocalDateTime.now(),
    var leaseUntil: LocalDateTime? = null,
    var leaseToken: UUID? = null,
    @Column(length = 2000)
    var lastError: String? = null,
    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(nullable = false, columnDefinition = "timestamp default current_timestamp")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "processing_results")
class ProcessingResultEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    var caseRequest: CaseRequestEntity,
    @Column(nullable = false)
    var runNumber: Int,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var decision: ProcessingDecision,
    @Column(columnDefinition = "TEXT")
    var reasonCodes: String = "[]",
    @Column(nullable = false)
    var rulesVersion: String = "DOCUMENTAL_V1",
    @Column(nullable = false)
    var evaluatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "case_histories")
class CaseHistoryEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    var caseRequest: CaseRequestEntity,
    @Column(nullable = false)
    var eventType: String,
    @Column(nullable = false)
    var actorSubject: String,
    @Column(length = 1000)
    var details: String? = null,
    @Column(nullable = false)
    var occurredAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "notifications")
class NotificationEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    var caseRequest: CaseRequestEntity,
    @Column(nullable = false)
    var recipientSubject: UUID,
    @Column(nullable = false)
    var title: String,
    @Column(nullable = false, length = 1000)
    var message: String,
    var readAt: LocalDateTime? = null,
    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(
    name = "idempotency_record",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_idempotency_operation_key",
            columnNames = ["operation", "idempotency_key"]
        )
    ]
)
class IdempotencyRecordEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 32)
    var operation: String,
    @Column(name = "idempotency_key", nullable = false, columnDefinition = "TEXT")
    var idempotencyKey: String,
    @Column(name = "request_hash", nullable = false, length = 64)
    var requestHash: String,
    @Column(name = "resource_id", nullable = false)
    var resourceId: UUID,
    @Column(name = "response_status", nullable = false)
    var responseStatus: Int,
    @Column(name = "response_body", nullable = false, columnDefinition = "TEXT")
    var responseBody: String,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)
