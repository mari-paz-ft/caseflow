package com.caseflow.bff.infrastructure.client.case

import com.caseflow.bff.application.dto.BffCaseDocumentDto
import com.caseflow.bff.application.dto.BffCaseHistoryDto
import com.caseflow.bff.application.dto.BffCaseResponseDto
import com.caseflow.bff.application.dto.BffNotificationDto
import com.caseflow.bff.application.dto.BffProcessingResultDto

object CaseServiceDtoMapper {
    fun toBffCase(source: CaseServiceCaseWire) = BffCaseResponseDto(
        id = source.id.toString(),
        protocol = source.protocol,
        ownerSubject = source.ownerSubject.toString(),
        ownerEmail = source.ownerEmail,
        title = source.title,
        description = source.description,
        caseType = source.caseType,
        status = source.status,
        version = source.version,
        processingRun = source.processingRun,
        rulesVersion = source.rulesVersion,
        submittedAt = source.submittedAt,
        createdAt = source.createdAt,
        updatedAt = source.updatedAt,
        documents = source.documents.map(::toBffDocument),
        latestResult = source.latestResult?.let(::toBffResult)
    )

    fun toBffDocument(source: CaseServiceDocumentWire) = BffCaseDocumentDto(
        id = source.id.toString(),
        category = source.category,
        fileName = source.fileName,
        fileSize = source.fileSize,
        contentType = source.contentType,
        validUntil = source.validUntil,
        uploadState = source.uploadState,
        createdAt = source.createdAt
    )

    fun toBffResult(source: CaseServiceResultWire) = BffProcessingResultDto(
        id = source.id.toString(),
        runNumber = source.runNumber,
        decision = source.decision,
        reasonCodes = source.reasonCodes,
        rulesVersion = source.rulesVersion,
        evaluatedAt = source.evaluatedAt
    )

    fun toBffHistory(source: CaseServiceHistoryWire) = BffCaseHistoryDto(
        id = source.id.toString(),
        caseId = source.caseId.toString(),
        eventType = source.eventType,
        actorSubject = source.actorSubject,
        details = source.details,
        occurredAt = source.occurredAt
    )

    fun toBffNotification(source: CaseServiceNotificationWire) = BffNotificationDto(
        id = source.id.toString(),
        caseId = source.caseId.toString(),
        title = source.title,
        message = source.message,
        readAt = source.readAt,
        createdAt = source.createdAt
    )
}
