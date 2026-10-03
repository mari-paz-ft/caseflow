package com.caseflow.service

import com.caseflow.controller.dto.NotificationDto
import com.caseflow.domain.model.CaseRequest
import com.caseflow.domain.model.Notification
import com.caseflow.repository.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

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
    fun getNotificationsForUser(userSubject: UUID): List<Notification> =
        notificationRepository.findByRecipientSubjectOrderByCreatedAtDesc(userSubject)

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
    fun countUnread(userSubject: UUID): Long =
        notificationRepository.countByRecipientSubjectAndReadAtIsNull(userSubject)

    fun toDto(notification: Notification): NotificationDto = NotificationDto(
        id = notification.id,
        caseId = notification.caseRequest.id,
        title = notification.title,
        message = notification.message,
        readAt = notification.readAt,
        createdAt = notification.createdAt
    )
}
