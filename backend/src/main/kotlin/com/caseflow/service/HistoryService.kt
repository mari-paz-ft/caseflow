package com.caseflow.service

import com.caseflow.controller.dto.CaseHistoryDto
import com.caseflow.domain.model.CaseHistory
import com.caseflow.domain.model.CaseRequest
import com.caseflow.repository.CaseHistoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class HistoryService(
    private val historyRepository: CaseHistoryRepository
) {
    @Transactional
    fun record(caseRequest: CaseRequest, eventType: String, actorSubject: String, details: String? = null): CaseHistory {
        val history = CaseHistory(
            caseRequest = caseRequest,
            eventType = eventType,
            actorSubject = actorSubject,
            details = details,
            occurredAt = LocalDateTime.now()
        )
        return historyRepository.save(history)
    }

    @Transactional(readOnly = true)
    fun getHistoryForCase(caseId: UUID): List<CaseHistory> =
        historyRepository.findByCaseRequestIdOrderByOccurredAtDesc(caseId)

    fun toDto(history: CaseHistory): CaseHistoryDto = CaseHistoryDto(
        id = history.id,
        caseId = history.caseRequest.id,
        eventType = history.eventType,
        actorSubject = history.actorSubject,
        details = history.details,
        occurredAt = history.occurredAt
    )
}
