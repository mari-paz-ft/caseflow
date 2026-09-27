package com.caseflow.infrastructure.scheduler

import com.caseflow.application.usecase.AnalysisEngineService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.UUID

class ProcessingJobSchedulerTest {
    @Test
    fun `poll delega o job adquirido ao motor de analise`() {
        val claimService = Mockito.mock(ProcessingJobClaimService::class.java)
        val analysisEngineService = Mockito.mock(AnalysisEngineService::class.java)
        val claim = ClaimedProcessingJob(UUID.randomUUID(), UUID.randomUUID())
        Mockito.`when`(claimService.claimNext()).thenReturn(claim)

        ProcessingJobScheduler(claimService, analysisEngineService).poll()

        Mockito.verify(analysisEngineService).processClaimedJob(claim.jobId, claim.leaseToken)
    }
}
