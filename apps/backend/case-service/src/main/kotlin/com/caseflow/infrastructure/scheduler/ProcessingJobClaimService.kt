package com.caseflow.infrastructure.scheduler

import com.caseflow.application.port.CaseRequestRepository
import com.caseflow.application.port.ProcessingJobRepository
import com.caseflow.application.usecase.HistoryService
import com.caseflow.domain.enums.CaseStatus
import com.caseflow.domain.enums.JobState
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class ProcessingJobClaimService(
    private val processingJobRepository: ProcessingJobRepository,
    private val caseRequestRepository: CaseRequestRepository,
    private val historyService: HistoryService,
    @Value("\${caseflow.scheduler.lease-seconds:30}") private val leaseSeconds: Long
) {
    @Transactional
    fun claimNext(): ClaimedProcessingJob? {
        require(leaseSeconds > 0) { "Scheduler lease duration must be positive" }
        val now = LocalDateTime.now()
        val job = processingJobRepository.findNextRunnableJobForUpdate(now).orElse(null) ?: return null

        val wasScheduled = job.state == JobState.SCHEDULED
        val leaseToken = UUID.randomUUID()
        job.state = JobState.RUNNING
        job.leaseToken = leaseToken
        job.leaseUntil = now.plusSeconds(leaseSeconds)
        job.updatedAt = now
        processingJobRepository.saveAndFlush(job)

        val caseRequest = job.caseRequest
        caseRequest.status = CaseStatus.PROCESSANDO
        caseRequest.updatedAt = now
        caseRequestRepository.save(caseRequest)
        if (wasScheduled) {
            historyService.record(
                caseRequest = caseRequest,
                eventType = "PROCESSAMENTO_INICIADO",
                actorSubject = "SYSTEM",
                details = "Executor interno assumiu execução #${job.runNumber}"
            )
        }

        return ClaimedProcessingJob(job.id, leaseToken)
    }
}
