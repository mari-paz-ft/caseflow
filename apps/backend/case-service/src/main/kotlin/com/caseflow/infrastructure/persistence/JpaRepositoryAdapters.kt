package com.caseflow.infrastructure.persistence

import com.caseflow.application.port.CaseDocumentRepository
import com.caseflow.application.port.CaseHistoryRepository
import com.caseflow.application.port.CaseRequestRepository
import com.caseflow.application.port.IdempotencyRecordRepository
import com.caseflow.application.port.NotificationRepository
import com.caseflow.application.port.ProcessingJobRepository
import com.caseflow.application.port.ProcessingResultRepository
import com.caseflow.domain.enums.DocumentCategory
import com.caseflow.domain.enums.JobState
import com.caseflow.domain.exception.ConflictException
import com.caseflow.domain.model.CaseDocument
import com.caseflow.domain.model.CaseHistory
import com.caseflow.domain.model.CaseRequest
import com.caseflow.domain.model.IdempotencyRecord
import com.caseflow.domain.model.Notification
import com.caseflow.domain.model.ProcessingJob
import com.caseflow.domain.model.ProcessingResult
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

@Repository
class JpaCaseRequestRepositoryAdapter(
    private val repository: CaseRequestJpaRepository
) : CaseRequestRepository {
    @Transactional
    override fun save(caseRequest: CaseRequest): CaseRequest {
        val existing = repository.findById(caseRequest.id).orElse(null)
        return repository.save(caseRequest.toEntity(existing)).toDomain()
    }

    @Transactional(readOnly = true)
    override fun findById(id: UUID): Optional<CaseRequest> = repository.findById(id).map { it.toDomain() }

    @Transactional
    override fun findByIdForUpdate(id: UUID): Optional<CaseRequest> = repository.findByIdForUpdate(id).map { it.toDomain() }

    @Transactional(readOnly = true)
    override fun findByOwnerSubjectOrderByCreatedAtDesc(ownerSubject: UUID): List<CaseRequest> =
        repository.findByOwnerSubjectOrderByCreatedAtDesc(ownerSubject).map { it.toDomain() }

    @Transactional(readOnly = true)
    override fun findAllByOrderByCreatedAtDesc(): List<CaseRequest> =
        repository.findAllByOrderByCreatedAtDesc().map { it.toDomain() }

    @Transactional(readOnly = true)
    override fun findByProtocol(protocol: String): Optional<CaseRequest> = repository.findByProtocol(protocol).map { it.toDomain() }

    @Transactional
    override fun remapOwnerSubject(oldSubject: UUID, newSubject: UUID): Int = repository.remapOwnerSubject(oldSubject, newSubject)

    @Transactional(readOnly = true)
    override fun count(): Long = repository.count()
}

@Repository
class JpaCaseDocumentRepositoryAdapter(
    private val repository: CaseDocumentJpaRepository,
    private val caseRequests: CaseRequestJpaRepository
) : CaseDocumentRepository {
    @Transactional
    override fun save(document: CaseDocument): CaseDocument {
        val parent = parentEntity(document.caseRequest)
        val existing = repository.findById(document.id).orElse(null)
        return repository.save(document.toEntity(parent, existing)).toDomain(parent.toDomain())
    }

    @Transactional
    override fun saveAndFlush(document: CaseDocument): CaseDocument {
        val parent = parentEntity(document.caseRequest)
        val existing = repository.findById(document.id).orElse(null)
        return repository.saveAndFlush(document.toEntity(parent, existing)).toDomain(parent.toDomain())
    }

    @Transactional(readOnly = true)
    override fun findById(id: UUID): Optional<CaseDocument> = repository.findById(id).map { it.toDomain() }

    @Transactional(readOnly = true)
    override fun findByCaseRequestId(caseId: UUID): List<CaseDocument> = repository.findByCaseRequestId(caseId).map { it.toDomain() }

    @Transactional(readOnly = true)
    override fun findByCaseRequestIdAndCategory(caseId: UUID, category: DocumentCategory): Optional<CaseDocument> =
        repository.findByCaseRequestIdAndCategory(caseId, category).map { it.toDomain() }

    @Transactional
    override fun deleteByCaseRequestIdAndId(caseId: UUID, id: UUID) = repository.deleteByCaseRequestIdAndId(caseId, id)

    @Transactional
    override fun delete(document: CaseDocument) = repository.deleteById(document.id)

    private fun parentEntity(parent: CaseRequest): CaseRequestEntity =
        caseRequests.findById(parent.id).orElseGet { parent.toEntity() }
}

@Repository
class JpaProcessingJobRepositoryAdapter(
    private val repository: ProcessingJobJpaRepository,
    private val caseRequests: CaseRequestJpaRepository
) : ProcessingJobRepository {
    @Transactional
    override fun save(job: ProcessingJob): ProcessingJob {
        val parent = parentEntity(job.caseRequest)
        val existing = repository.findById(job.id).orElse(null)
        return repository.save(job.toEntity(parent, existing)).toDomain(parent.toDomain())
    }

    @Transactional
    override fun saveAndFlush(job: ProcessingJob): ProcessingJob {
        val parent = parentEntity(job.caseRequest)
        val existing = repository.findById(job.id).orElse(null)
        return repository.saveAndFlush(job.toEntity(parent, existing)).toDomain(parent.toDomain())
    }

    @Transactional(readOnly = true)
    override fun findById(id: UUID): Optional<ProcessingJob> = repository.findById(id).map { it.toDomain() }

    @Transactional
    override fun findByIdForUpdate(id: UUID): Optional<ProcessingJob> = repository.findByIdForUpdate(id).map { it.toDomain() }

    @Transactional(readOnly = true)
    override fun findByCaseRequestId(caseId: UUID): List<ProcessingJob> =
        repository.findByCaseRequestId(caseId).map { it.toDomain() }

    @Transactional
    override fun findNextRunnableJobForUpdate(now: LocalDateTime): Optional<ProcessingJob> = repository.findRunnableJobs(
        scheduled = JobState.SCHEDULED,
        running = JobState.RUNNING,
        now = now,
        pageable = PageRequest.of(0, 1)
    ).firstOrNull()?.let { Optional.of(it.toDomain()) } ?: Optional.empty()

    @Transactional(readOnly = true)
    override fun findByState(state: JobState): List<ProcessingJob> = repository.findByState(state).map { it.toDomain() }

    private fun parentEntity(parent: CaseRequest): CaseRequestEntity =
        caseRequests.findById(parent.id).orElseGet { parent.toEntity() }
}

@Repository
class JpaProcessingResultRepositoryAdapter(
    private val repository: ProcessingResultJpaRepository,
    private val caseRequests: CaseRequestJpaRepository
) : ProcessingResultRepository {
    @Transactional
    override fun save(result: ProcessingResult): ProcessingResult {
        val parent = parentEntity(result.caseRequest)
        val existing = repository.findById(result.id).orElse(null)
        return repository.save(result.toEntity(parent, existing)).toDomain(parent.toDomain())
    }

    @Transactional(readOnly = true)
    override fun findByCaseRequestIdOrderByRunNumberDesc(caseId: UUID): List<ProcessingResult> =
        repository.findByCaseRequestIdOrderByRunNumberDesc(caseId).map { it.toDomain() }

    @Transactional(readOnly = true)
    override fun findFirstByCaseRequestIdOrderByRunNumberDesc(caseId: UUID): Optional<ProcessingResult> =
        repository.findFirstByCaseRequestIdOrderByRunNumberDesc(caseId).map { it.toDomain() }

    private fun parentEntity(parent: CaseRequest): CaseRequestEntity =
        caseRequests.findById(parent.id).orElseGet { parent.toEntity() }
}

@Repository
class JpaCaseHistoryRepositoryAdapter(
    private val repository: CaseHistoryJpaRepository,
    private val caseRequests: CaseRequestJpaRepository
) : CaseHistoryRepository {
    @Transactional
    override fun save(history: CaseHistory): CaseHistory {
        val parent = caseRequests.findById(history.caseRequest.id).orElseGet { history.caseRequest.toEntity() }
        val existing = repository.findById(history.id).orElse(null)
        return repository.save(history.toEntity(parent, existing)).toDomain(parent.toDomain())
    }

    @Transactional(readOnly = true)
    override fun findByCaseRequestIdOrderByOccurredAtDesc(caseId: UUID): List<CaseHistory> =
        repository.findByCaseRequestIdOrderByOccurredAtDesc(caseId).map { it.toDomain() }
}

@Repository
class JpaNotificationRepositoryAdapter(
    private val repository: NotificationJpaRepository,
    private val caseRequests: CaseRequestJpaRepository
) : NotificationRepository {
    @Transactional
    override fun save(notification: Notification): Notification {
        val parent = caseRequests.findById(notification.caseRequest.id).orElseGet { notification.caseRequest.toEntity() }
        val existing = repository.findById(notification.id).orElse(null)
        return repository.save(notification.toEntity(parent, existing)).toDomain(parent.toDomain())
    }

    @Transactional(readOnly = true)
    override fun findById(id: UUID): Optional<Notification> = repository.findById(id).map { it.toDomain() }

    @Transactional(readOnly = true)
    override fun findByRecipientSubjectOrderByCreatedAtDesc(recipientSubject: UUID): List<Notification> =
        repository.findByRecipientSubjectOrderByCreatedAtDesc(recipientSubject).map { it.toDomain() }

    @Transactional(readOnly = true)
    override fun countByRecipientSubjectAndReadAtIsNull(recipientSubject: UUID): Long =
        repository.countByRecipientSubjectAndReadAtIsNull(recipientSubject)

    @Transactional
    override fun remapRecipientSubject(oldSubject: UUID, newSubject: UUID): Int =
        repository.remapRecipientSubject(oldSubject, newSubject)
}

@Repository
class JpaIdempotencyRecordRepositoryAdapter(
    private val repository: IdempotencyRecordJpaRepository
) : IdempotencyRecordRepository {
    @Transactional
    override fun saveAndFlush(record: IdempotencyRecord): IdempotencyRecord = try {
        val existing = repository.findById(record.id).orElse(null)
        repository.saveAndFlush(record.toEntity(existing)).toDomain()
    } catch (_: DataIntegrityViolationException) {
        throw ConflictException("Idempotency-Key já foi utilizada por outra operação", "IDEMPOTENCY_KEY_REUSED")
    }

    @Transactional(readOnly = true)
    override fun findByOperationAndIdempotencyKey(operation: String, idempotencyKey: String): Optional<IdempotencyRecord> =
        repository.findByOperationAndIdempotencyKey(operation, idempotencyKey).map { it.toDomain() }

    @Transactional(readOnly = true)
    override fun countByOperationAndIdempotencyKey(operation: String, idempotencyKey: String): Long =
        repository.countByOperationAndIdempotencyKey(operation, idempotencyKey)
}
