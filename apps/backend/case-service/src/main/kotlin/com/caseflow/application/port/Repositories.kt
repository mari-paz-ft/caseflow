package com.caseflow.application.port

import com.caseflow.domain.enums.DocumentCategory
import com.caseflow.domain.model.CaseDocument
import com.caseflow.domain.model.CaseHistory
import com.caseflow.domain.model.CaseRequest
import com.caseflow.domain.model.IdempotencyRecord
import com.caseflow.domain.model.Notification
import com.caseflow.domain.model.ProcessingJob
import com.caseflow.domain.model.ProcessingResult
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

interface CaseRequestRepository {
    fun save(caseRequest: CaseRequest): CaseRequest
    fun findById(id: UUID): Optional<CaseRequest>
    fun findByIdForUpdate(id: UUID): Optional<CaseRequest>
    fun findByOwnerSubjectOrderByCreatedAtDesc(ownerSubject: UUID): List<CaseRequest>
    fun findAllByOrderByCreatedAtDesc(): List<CaseRequest>
    fun findByProtocol(protocol: String): Optional<CaseRequest>
    fun remapOwnerSubject(oldSubject: UUID, newSubject: UUID): Int
    fun count(): Long
}

interface CaseDocumentRepository {
    fun save(document: CaseDocument): CaseDocument
    fun saveAndFlush(document: CaseDocument): CaseDocument
    fun findById(id: UUID): Optional<CaseDocument>
    fun findByCaseRequestId(caseId: UUID): List<CaseDocument>
    fun findByCaseRequestIdAndCategory(caseId: UUID, category: DocumentCategory): Optional<CaseDocument>
    fun deleteByCaseRequestIdAndId(caseId: UUID, id: UUID)
    fun delete(document: CaseDocument)
}

interface ProcessingJobRepository {
    fun save(job: ProcessingJob): ProcessingJob
    fun saveAndFlush(job: ProcessingJob): ProcessingJob
    fun findById(id: UUID): Optional<ProcessingJob>
    fun findByIdForUpdate(id: UUID): Optional<ProcessingJob>
    fun findByCaseRequestId(caseId: UUID): List<ProcessingJob>
    fun findNextRunnableJobForUpdate(now: LocalDateTime): Optional<ProcessingJob>
    fun findByState(state: com.caseflow.domain.enums.JobState): List<ProcessingJob>
}

interface ProcessingResultRepository {
    fun save(result: ProcessingResult): ProcessingResult
    fun findByCaseRequestIdOrderByRunNumberDesc(caseId: UUID): List<ProcessingResult>
    fun findFirstByCaseRequestIdOrderByRunNumberDesc(caseId: UUID): Optional<ProcessingResult>
}

interface CaseHistoryRepository {
    fun save(history: CaseHistory): CaseHistory
    fun findByCaseRequestIdOrderByOccurredAtDesc(caseId: UUID): List<CaseHistory>
}

interface NotificationRepository {
    fun save(notification: Notification): Notification
    fun findById(id: UUID): Optional<Notification>
    fun findByRecipientSubjectOrderByCreatedAtDesc(recipientSubject: UUID): List<Notification>
    fun countByRecipientSubjectAndReadAtIsNull(recipientSubject: UUID): Long
    fun remapRecipientSubject(oldSubject: UUID, newSubject: UUID): Int
}

interface IdempotencyRecordRepository {
    fun saveAndFlush(record: IdempotencyRecord): IdempotencyRecord
    fun findByOperationAndIdempotencyKey(operation: String, idempotencyKey: String): Optional<IdempotencyRecord>
    fun countByOperationAndIdempotencyKey(operation: String, idempotencyKey: String): Long
}
