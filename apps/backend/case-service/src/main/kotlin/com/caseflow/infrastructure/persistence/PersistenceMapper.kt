package com.caseflow.infrastructure.persistence

import com.caseflow.domain.model.CaseDocument
import com.caseflow.domain.model.CaseHistory
import com.caseflow.domain.model.CaseRequest
import com.caseflow.domain.model.IdempotencyRecord
import com.caseflow.domain.model.Notification
import com.caseflow.domain.model.ProcessingJob
import com.caseflow.domain.model.ProcessingResult

internal fun CaseRequestEntity.toDomain(): CaseRequest {
    val model = CaseRequest(
        id = id,
        protocol = protocol,
        ownerSubject = ownerSubject,
        ownerEmail = ownerEmail,
        title = title,
        description = description,
        caseType = caseType,
        status = status,
        version = version,
        processingRun = processingRun,
        rulesVersion = rulesVersion,
        submittedAt = submittedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    model.documents.addAll(documents.map { it.toDomain(model) })
    model.histories.addAll(histories.map { it.toDomain(model) })
    model.results.addAll(results.map { it.toDomain(model) })
    return model
}

internal fun CaseRequest.toEntity(existing: CaseRequestEntity? = null): CaseRequestEntity {
    val entity = existing ?: CaseRequestEntity(id = id, version = version)
    entity.protocol = protocol
    entity.ownerSubject = ownerSubject
    entity.ownerEmail = ownerEmail
    entity.title = title
    entity.description = description
    entity.caseType = caseType
    entity.status = status
    if (existing == null) entity.version = version
    entity.processingRun = processingRun
    entity.rulesVersion = rulesVersion
    entity.submittedAt = submittedAt
    entity.createdAt = createdAt
    entity.updatedAt = updatedAt

    val requestedDocuments = documents.associateBy { it.id }
    entity.documents.removeAll { it.id !in requestedDocuments }
    documents.forEach { document ->
        val current = entity.documents.firstOrNull { it.id == document.id }
        if (current == null) {
            entity.documents.add(document.toEntity(entity))
        } else {
            document.updateEntity(current)
        }
    }
    return entity
}

internal fun CaseDocumentEntity.toDomain(parent: CaseRequest = caseRequest.toDomain()) = CaseDocument(
    id = id,
    caseRequest = parent,
    category = category,
    fileName = fileName,
    fileSize = fileSize,
    contentType = contentType,
    storageKey = storageKey,
    sha256 = sha256,
    validUntil = validUntil,
    uploadState = uploadState,
    createdAt = createdAt
)

internal fun CaseDocument.toEntity(parent: CaseRequestEntity, existing: CaseDocumentEntity? = null): CaseDocumentEntity {
    val entity = existing ?: CaseDocumentEntity(id = id, caseRequest = parent, category = category)
    updateEntity(entity)
    entity.caseRequest = parent
    return entity
}

private fun CaseDocument.updateEntity(entity: CaseDocumentEntity) {
    entity.category = category
    entity.fileName = fileName
    entity.fileSize = fileSize
    entity.contentType = contentType
    entity.storageKey = storageKey
    entity.sha256 = sha256
    entity.validUntil = validUntil
    entity.uploadState = uploadState
    entity.createdAt = createdAt
}

internal fun ProcessingJobEntity.toDomain(parent: CaseRequest = caseRequest.toDomain()) = ProcessingJob(
    id = id,
    caseRequest = parent,
    runNumber = runNumber,
    state = state,
    attemptCount = attemptCount,
    availableAt = availableAt,
    leaseUntil = leaseUntil,
    leaseToken = leaseToken,
    lastError = lastError,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun ProcessingJob.toEntity(parent: CaseRequestEntity, existing: ProcessingJobEntity? = null): ProcessingJobEntity {
    val entity = existing ?: ProcessingJobEntity(id = id, caseRequest = parent, runNumber = runNumber)
    entity.caseRequest = parent
    entity.runNumber = runNumber
    entity.state = state
    entity.attemptCount = attemptCount
    entity.availableAt = availableAt
    entity.leaseUntil = leaseUntil
    entity.leaseToken = leaseToken
    entity.lastError = lastError
    entity.createdAt = createdAt
    entity.updatedAt = updatedAt
    return entity
}

internal fun ProcessingResultEntity.toDomain(parent: CaseRequest = caseRequest.toDomain()) = ProcessingResult(
    id = id,
    caseRequest = parent,
    runNumber = runNumber,
    decision = decision,
    reasonCodes = reasonCodes,
    rulesVersion = rulesVersion,
    evaluatedAt = evaluatedAt
)

internal fun ProcessingResult.toEntity(parent: CaseRequestEntity, existing: ProcessingResultEntity? = null): ProcessingResultEntity {
    val entity = existing ?: ProcessingResultEntity(id = id, caseRequest = parent, runNumber = runNumber, decision = decision)
    entity.caseRequest = parent
    entity.runNumber = runNumber
    entity.decision = decision
    entity.reasonCodes = reasonCodes
    entity.rulesVersion = rulesVersion
    entity.evaluatedAt = evaluatedAt
    return entity
}

internal fun CaseHistoryEntity.toDomain(parent: CaseRequest = caseRequest.toDomain()) = CaseHistory(
    id = id,
    caseRequest = parent,
    eventType = eventType,
    actorSubject = actorSubject,
    details = details,
    occurredAt = occurredAt
)

internal fun CaseHistory.toEntity(parent: CaseRequestEntity, existing: CaseHistoryEntity? = null): CaseHistoryEntity {
    val entity = existing ?: CaseHistoryEntity(id = id, caseRequest = parent, eventType = eventType, actorSubject = actorSubject)
    entity.caseRequest = parent
    entity.eventType = eventType
    entity.actorSubject = actorSubject
    entity.details = details
    entity.occurredAt = occurredAt
    return entity
}

internal fun NotificationEntity.toDomain(parent: CaseRequest = caseRequest.toDomain()) = Notification(
    id = id,
    caseRequest = parent,
    recipientSubject = recipientSubject,
    title = title,
    message = message,
    readAt = readAt,
    createdAt = createdAt
)

internal fun Notification.toEntity(parent: CaseRequestEntity, existing: NotificationEntity? = null): NotificationEntity {
    val entity = existing ?: NotificationEntity(
        id = id,
        caseRequest = parent,
        recipientSubject = recipientSubject,
        title = title,
        message = message
    )
    entity.caseRequest = parent
    entity.recipientSubject = recipientSubject
    entity.title = title
    entity.message = message
    entity.readAt = readAt
    entity.createdAt = createdAt
    return entity
}

internal fun IdempotencyRecordEntity.toDomain() = IdempotencyRecord(
    id = id,
    operation = operation,
    idempotencyKey = idempotencyKey,
    requestHash = requestHash,
    resourceId = resourceId,
    responseStatus = responseStatus,
    responseBody = responseBody,
    createdAt = createdAt
)

internal fun IdempotencyRecord.toEntity(existing: IdempotencyRecordEntity? = null): IdempotencyRecordEntity {
    val entity = existing ?: IdempotencyRecordEntity(
        id = id,
        operation = operation,
        idempotencyKey = idempotencyKey,
        requestHash = requestHash,
        resourceId = resourceId,
        responseStatus = responseStatus,
        responseBody = responseBody,
        createdAt = createdAt
    )
    entity.operation = operation
    entity.idempotencyKey = idempotencyKey
    entity.requestHash = requestHash
    entity.resourceId = resourceId
    entity.responseStatus = responseStatus
    entity.responseBody = responseBody
    entity.createdAt = createdAt
    return entity
}
