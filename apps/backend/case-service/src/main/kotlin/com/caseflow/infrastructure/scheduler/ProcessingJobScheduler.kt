package com.caseflow.infrastructure.scheduler

import com.caseflow.application.usecase.AnalysisEngineService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["caseflow.scheduler.enabled"], havingValue = "true", matchIfMissing = true)
class ProcessingJobScheduler(
    private val claimService: ProcessingJobClaimService,
    private val analysisEngineService: AnalysisEngineService
) {
    @Scheduled(fixedDelayString = "\${caseflow.scheduler.poll-interval-ms:1000}")
    fun poll() {
        val job = claimService.claimNext() ?: return
        analysisEngineService.processClaimedJob(job.jobId, job.leaseToken)
    }
}
