package com.caseflow.repository

import com.caseflow.domain.enums.DocumentCategory
import com.caseflow.domain.enums.JobState
import com.caseflow.domain.model.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface CaseRequestRepository : JpaRepository<CaseRequest, UUID> {
    fun findByOwnerSubjectOrderByCreatedAtDesc(ownerSubject: UUID): List<CaseRequest>
    fun findAllByOrderByCreatedAtDesc(): List<CaseRequest>
    fun findByProtocol(protocol: String): Optional<CaseRequest>
}

@Repository
interface CaseDocumentRepository : JpaRepository<CaseDocument, UUID> {
    fun findByCaseRequestId(caseId: UUID): List<CaseDocument>
    fun findByCaseRequestIdAndCategory(caseId: UUID, category: DocumentCategory): Optional<CaseDocument>
    fun deleteByCaseRequestIdAndId(caseId: UUID, id: UUID)
}

@Repository
interface ProcessingJobRepository : JpaRepository<ProcessingJob, UUID> {
    fun findByCaseRequestId(caseId: UUID): List<ProcessingJob>
    fun findByState(state: JobState): List<ProcessingJob>
}

@Repository
interface ProcessingResultRepository : JpaRepository<ProcessingResult, UUID> {
    fun findByCaseRequestIdOrderByRunNumberDesc(caseId: UUID): List<ProcessingResult>
    fun findFirstByCaseRequestIdOrderByRunNumberDesc(caseId: UUID): Optional<ProcessingResult>
}

@Repository
interface CaseHistoryRepository : JpaRepository<CaseHistory, UUID> {
    fun findByCaseRequestIdOrderByOccurredAtDesc(caseId: UUID): List<CaseHistory>
}

@Repository
interface NotificationRepository : JpaRepository<Notification, UUID> {
    fun findByRecipientSubjectOrderByCreatedAtDesc(recipientSubject: UUID): List<Notification>
    fun countByRecipientSubjectAndReadAtIsNull(recipientSubject: UUID): Long
}

@Repository
interface AppUserRepository : JpaRepository<AppUser, UUID> {
    fun findByEmail(email: String): Optional<AppUser>
}
