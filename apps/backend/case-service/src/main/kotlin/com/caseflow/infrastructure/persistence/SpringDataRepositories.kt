package com.caseflow.infrastructure.persistence

import com.caseflow.domain.enums.DocumentCategory
import com.caseflow.domain.enums.JobState
import jakarta.persistence.LockModeType
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

interface CaseRequestJpaRepository : JpaRepository<CaseRequestEntity, UUID> {
    fun findByOwnerSubjectOrderByCreatedAtDesc(ownerSubject: UUID): List<CaseRequestEntity>
    fun findAllByOrderByCreatedAtDesc(): List<CaseRequestEntity>
    fun findByProtocol(protocol: String): Optional<CaseRequestEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CaseRequestEntity c where c.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): Optional<CaseRequestEntity>

    @Modifying
    @Query("update CaseRequestEntity c set c.ownerSubject = :newSubject where c.ownerSubject = :oldSubject")
    fun remapOwnerSubject(@Param("oldSubject") oldSubject: UUID, @Param("newSubject") newSubject: UUID): Int
}

interface CaseDocumentJpaRepository : JpaRepository<CaseDocumentEntity, UUID> {
    fun findByCaseRequestId(caseId: UUID): List<CaseDocumentEntity>
    fun findByCaseRequestIdAndCategory(caseId: UUID, category: DocumentCategory): Optional<CaseDocumentEntity>
    fun deleteByCaseRequestIdAndId(caseId: UUID, id: UUID)
}

interface ProcessingJobJpaRepository : JpaRepository<ProcessingJobEntity, UUID> {
    fun findByCaseRequestId(caseId: UUID): List<ProcessingJobEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from ProcessingJobEntity j where j.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): Optional<ProcessingJobEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select j from ProcessingJobEntity j
        where j.availableAt <= :now
          and (j.state = :scheduled or j.state = :running)
          and (j.leaseUntil is null or j.leaseUntil <= :now)
        order by j.availableAt asc, j.createdAt asc
        """)
    fun findRunnableJobs(
        @Param("scheduled") scheduled: JobState,
        @Param("running") running: JobState,
        @Param("now") now: LocalDateTime,
        pageable: PageRequest
    ): List<ProcessingJobEntity>

    fun findByState(state: JobState): List<ProcessingJobEntity>
}

interface ProcessingResultJpaRepository : JpaRepository<ProcessingResultEntity, UUID> {
    fun findByCaseRequestIdOrderByRunNumberDesc(caseId: UUID): List<ProcessingResultEntity>
    fun findFirstByCaseRequestIdOrderByRunNumberDesc(caseId: UUID): Optional<ProcessingResultEntity>
}

interface CaseHistoryJpaRepository : JpaRepository<CaseHistoryEntity, UUID> {
    fun findByCaseRequestIdOrderByOccurredAtDesc(caseId: UUID): List<CaseHistoryEntity>
}

interface NotificationJpaRepository : JpaRepository<NotificationEntity, UUID> {
    fun findByRecipientSubjectOrderByCreatedAtDesc(recipientSubject: UUID): List<NotificationEntity>
    fun countByRecipientSubjectAndReadAtIsNull(recipientSubject: UUID): Long

    @Modifying
    @Query("update NotificationEntity n set n.recipientSubject = :newSubject where n.recipientSubject = :oldSubject")
    fun remapRecipientSubject(@Param("oldSubject") oldSubject: UUID, @Param("newSubject") newSubject: UUID): Int
}

interface IdempotencyRecordJpaRepository : JpaRepository<IdempotencyRecordEntity, UUID> {
    fun findByOperationAndIdempotencyKey(operation: String, idempotencyKey: String): Optional<IdempotencyRecordEntity>
    fun countByOperationAndIdempotencyKey(operation: String, idempotencyKey: String): Long
}
