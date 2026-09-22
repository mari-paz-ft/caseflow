package com.caseflow.service

import com.caseflow.domain.model.CaseHistory
import com.caseflow.domain.model.CaseRequest
import com.caseflow.domain.model.Notification
import com.caseflow.repository.CaseHistoryRepository
import com.caseflow.repository.NotificationRepository
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
    fun getHistoryForCase(caseId: UUID): List<CaseHistory> {
        return historyRepository.findByCaseRequestIdOrderByOccurredAtDesc(caseId)
    }
}

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository
) {
    @Transactional
    fun notify(caseRequest: CaseRequest, recipientSubject: UUID, title: String, message: String): Notification {
        val notification = Notification(
            caseRequest = caseRequest,
            recipientSubject = recipientSubject,
            title = title,
            message = message
        )
        return notificationRepository.save(notification)
    }

    @Transactional(readOnly = true)
    fun getNotificationsForUser(userSubject: UUID): List<Notification> {
        return notificationRepository.findByRecipientSubjectOrderByCreatedAtDesc(userSubject)
    }

    @Transactional
    fun markAsRead(notificationId: UUID, userSubject: UUID): Notification {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { IllegalArgumentException("Notificação não encontrada") }
        if (notification.recipientSubject != userSubject) {
            throw IllegalStateException("Acesso não autorizado à notificação")
        }
        notification.readAt = LocalDateTime.now()
        return notificationRepository.save(notification)
    }

    @Transactional(readOnly = true)
    fun countUnread(userSubject: UUID): Long {
        return notificationRepository.countByRecipientSubjectAndReadAtIsNull(userSubject)
    }
}
