package com.caseflow.infrastructure.scheduler

import com.caseflow.application.port.CaseRequestRepository
import com.caseflow.application.port.CaseHistoryRepository
import com.caseflow.application.port.ProcessingJobRepository
import com.caseflow.domain.enums.CaseStatus
import com.caseflow.domain.enums.JobState
import com.caseflow.domain.model.CaseRequest
import com.caseflow.domain.model.ProcessingJob
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
@ActiveProfiles("test")
class ProcessingJobRecoveryTest {
    @Autowired
    private lateinit var processingJobClaimService: ProcessingJobClaimService

    @Autowired
    private lateinit var processingJobRepository: ProcessingJobRepository

    @Autowired
    private lateinit var caseRequestRepository: CaseRequestRepository

    @Autowired
    private lateinit var caseHistoryRepository: CaseHistoryRepository

    @Test
    fun `claim adquire job scheduled e recupera running com lease expirado`() {
        val now = LocalDateTime.now()
        val caseRequest = caseRequestRepository.save(
            CaseRequest(
                protocol = "CF-SCHED-${UUID.randomUUID()}",
                ownerSubject = UUID.randomUUID(),
                ownerEmail = "scheduler-test",
                title = "Job persistido de teste",
                description = "Caso de teste para verificar scheduler persistido e recovery.",
                status = CaseStatus.ENVIADA,
                submittedAt = now
            )
        )
        val job = processingJobRepository.saveAndFlush(
            ProcessingJob(
                caseRequest = caseRequest,
                runNumber = 1,
                state = JobState.SCHEDULED,
                availableAt = now.minusSeconds(5)
            )
        )

        val firstClaim = requireNotNull(processingJobClaimService.claimNext())
        assertEquals(job.id, firstClaim.jobId)

        val claimed = processingJobRepository.findById(job.id).orElseThrow()
        assertEquals(CaseStatus.PROCESSANDO, caseRequestRepository.findById(caseRequest.id).orElseThrow().status)
        assertEquals(JobState.RUNNING, claimed.state)
        assertEquals(firstClaim.leaseToken, claimed.leaseToken)
        assertTrue(requireNotNull(claimed.leaseUntil).isAfter(LocalDateTime.now()))

        val expiredToken = requireNotNull(claimed.leaseToken)
        claimed.leaseUntil = LocalDateTime.now().minusSeconds(1)
        claimed.updatedAt = LocalDateTime.now()
        processingJobRepository.saveAndFlush(claimed)

        val recoveredClaim = requireNotNull(processingJobClaimService.claimNext())
        assertEquals(job.id, recoveredClaim.jobId)
        assertNotEquals(expiredToken, recoveredClaim.leaseToken)
        val recovered = processingJobRepository.findById(job.id).orElseThrow()
        assertEquals(CaseStatus.PROCESSANDO, caseRequestRepository.findById(caseRequest.id).orElseThrow().status)
        assertEquals(JobState.RUNNING, recovered.state)
        assertTrue(requireNotNull(recovered.leaseUntil).isAfter(LocalDateTime.now()))
    }

    @Test
    fun `workers concorrentes reivindicam o job uma unica vez`() {
        val now = LocalDateTime.now()
        val caseRequest = caseRequestRepository.save(
            CaseRequest(
                protocol = "CF-CONCURRENT-${UUID.randomUUID()}",
                ownerSubject = UUID.randomUUID(),
                ownerEmail = "scheduler-concurrent-test",
                title = "Job concorrente persistido",
                description = "Caso para verificar claim concorrente de workers.",
                status = CaseStatus.ENVIADA,
                submittedAt = now
            )
        )
        val job = processingJobRepository.saveAndFlush(
            ProcessingJob(
                caseRequest = caseRequest,
                runNumber = 1,
                state = JobState.SCHEDULED,
                availableAt = now.minusSeconds(5)
            )
        )
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val claims = (1..2).map {
                executor.submit<ClaimedProcessingJob?> {
                    start.await()
                    processingJobClaimService.claimNext()
                }
            }
            start.countDown()
            val results = claims.map { it.get(10, TimeUnit.SECONDS) }.filterNotNull()

            assertEquals(1, results.size)
            assertEquals(job.id, results.single().jobId)
            val persisted = processingJobRepository.findById(job.id).orElseThrow()
            assertEquals(JobState.RUNNING, persisted.state)
            assertEquals(results.single().leaseToken, persisted.leaseToken)
            assertEquals(
                1,
                caseHistoryRepository.findByCaseRequestIdOrderByOccurredAtDesc(caseRequest.id)
                    .count { it.eventType == "PROCESSAMENTO_INICIADO" }
            )
        } finally {
            executor.shutdownNow()
        }
    }
}
