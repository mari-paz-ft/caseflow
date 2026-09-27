package com.caseflow.infrastructure.persistence

import com.caseflow.application.port.CaseRequestRepository
import com.caseflow.application.port.NotificationRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class LegacySubjectMigration(
    private val caseRequestRepository: CaseRequestRepository,
    private val notificationRepository: NotificationRepository
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        val subjects = listOf(
            UUID.fromString("11111111-1111-1111-1111-111111111111") to
                DemoCaseSubjects.fromUsername(DemoCaseSubjects.USERNAME),
            UUID.fromString("99999999-9999-9999-9999-999999999999") to
                DemoCaseSubjects.fromUsername(DemoCaseSubjects.ADMIN_USERNAME)
        )

        subjects.forEach { (legacySubject, currentSubject) ->
            caseRequestRepository.remapOwnerSubject(legacySubject, currentSubject)
            notificationRepository.remapRecipientSubject(legacySubject, currentSubject)
        }
    }
}
