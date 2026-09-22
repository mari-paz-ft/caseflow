package com.caseflow.domain.model

import com.caseflow.domain.enums.*
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "case_requests")
class CaseRequest(
    @Id
    val id: UUID = UUID.randomUUID(),

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
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @OneToMany(mappedBy = "caseRequest", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val documents: MutableList<CaseDocument> = mutableListOf()

    @OneToMany(mappedBy = "caseRequest", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val histories: MutableList<CaseHistory> = mutableListOf()

    @OneToMany(mappedBy = "caseRequest", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val results: MutableList<ProcessingResult> = mutableListOf()
}

@Entity
@Table(name = "case_documents")
class CaseDocument(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    val caseRequest: CaseRequest,

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
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "processing_jobs")
class ProcessingJob(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    val caseRequest: CaseRequest,

    @Column(nullable = false)
    val runNumber: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var state: JobState = JobState.SCHEDULED,

    @Column(nullable = false)
    var attemptCount: Int = 0,

    @Column(nullable = false)
    var availableAt: LocalDateTime = LocalDateTime.now(),

    var leaseUntil: LocalDateTime? = null,

    var leaseToken: UUID? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "processing_results")
class ProcessingResult(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    val caseRequest: CaseRequest,

    @Column(nullable = false)
    val runNumber: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val decision: ProcessingDecision,

    @Column(columnDefinition = "TEXT")
    val reasonCodes: String = "[]",

    @Column(nullable = false)
    val rulesVersion: String = "DOCUMENTAL_V1",

    @Column(nullable = false)
    val evaluatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "case_histories")
class CaseHistory(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    val caseRequest: CaseRequest,

    @Column(nullable = false)
    val eventType: String,

    @Column(nullable = false)
    val actorSubject: String,

    @Column(length = 1000)
    val details: String? = null,

    @Column(nullable = false)
    val occurredAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "notifications")
class Notification(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    val caseRequest: CaseRequest,

    @Column(nullable = false)
    val recipientSubject: UUID,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, length = 1000)
    val message: String,

    var readAt: LocalDateTime? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "app_users")
class AppUser(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    val fullName: String,

    @Column(nullable = false)
    val passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: RoleName,

    @Column(nullable = false)
    val active: Boolean = true
)
