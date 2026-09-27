package com.caseflow.application.usecase

import com.caseflow.application.port.CaseDocumentRepository
import com.caseflow.application.port.CaseRequestRepository
import com.caseflow.application.port.DocumentStoragePort
import com.caseflow.application.port.NotificationRepository
import com.caseflow.application.port.ProcessingJobRepository
import com.caseflow.application.port.ProcessingResultRepository
import com.caseflow.domain.enums.CaseStatus
import com.caseflow.domain.enums.DocumentCategory
import com.caseflow.domain.enums.JobState
import com.caseflow.domain.enums.ProcessingDecision
import com.caseflow.domain.enums.UploadState
import com.caseflow.domain.model.CaseDocument
import com.caseflow.domain.model.CaseRequest
import com.caseflow.domain.model.ProcessingJob
import com.caseflow.domain.rule.PdfDocumentRules
import com.caseflow.infrastructure.scheduler.ProcessingJobClaimService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
class ProcessingJobRetryTest {
    @Autowired
    private lateinit var processingJobClaimService: ProcessingJobClaimService

    @Autowired
    private lateinit var analysisEngineService: AnalysisEngineService

    @Autowired
    private lateinit var caseRequestRepository: CaseRequestRepository

    @Autowired
    private lateinit var caseDocumentRepository: CaseDocumentRepository

    @Autowired
    private lateinit var documentStoragePort: DocumentStoragePort

    @Autowired
    private lateinit var processingJobRepository: ProcessingJobRepository

    @Autowired
    private lateinit var processingResultRepository: ProcessingResultRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    private lateinit var caseRequest: CaseRequest

    @BeforeEach
    fun setUp() {
        caseRequest = caseRequestRepository.save(
            CaseRequest(
                protocol = "CF-RETRY-${UUID.randomUUID()}",
                ownerSubject = UUID.randomUUID(),
                ownerEmail = "technical-retry-test",
                title = "[SIMULAR_FALHA] Documento inacessível",
                description = "Descrição de teste para falha técnica e política de retry.",
                status = CaseStatus.ENVIADA,
                processingRun = 1,
                submittedAt = LocalDateTime.now()
            )
        )
        processingJobRepository.saveAndFlush(
            ProcessingJob(
                caseRequest = caseRequest,
                runNumber = caseRequest.processingRun,
                state = JobState.SCHEDULED,
                availableAt = LocalDateTime.now().minusSeconds(1)
            )
        )
    }

    @Test
    fun `falhas tecnicas reagendam duas vezes e encerram na terceira`() {
        val expectedDelays = listOf(10L, 30L)

        repeat(3) { index ->
            val claim = requireNotNull(processingJobClaimService.claimNext())
            analysisEngineService.processClaimedJob(claim.jobId, claim.leaseToken)
            val job = processingJobRepository.findById(claim.jobId).orElseThrow()
            val failureNumber = index + 1

            assertEquals(failureNumber, job.attemptCount)
            assertNotNull(job.lastError)
            assertEquals(failureNumber.toLong(), notificationRepository
                .findByRecipientSubjectOrderByCreatedAtDesc(caseRequest.ownerSubject).size.toLong())

            if (failureNumber < 3) {
                assertEquals(JobState.SCHEDULED, job.state)
                assertEquals(CaseStatus.PROCESSANDO, caseRequestRepository.findById(caseRequest.id).orElseThrow().status)
                assertEquals(expectedDelays[index], java.time.Duration.between(job.updatedAt, job.availableAt).seconds)
                assertTrue(processingResultRepository.findByCaseRequestIdOrderByRunNumberDesc(caseRequest.id).isEmpty())
                job.availableAt = LocalDateTime.now().minusSeconds(1)
                processingJobRepository.saveAndFlush(job)
            } else {
                assertEquals(JobState.FAILED, job.state)
                assertEquals(CaseStatus.FALHA_TECNICA, caseRequestRepository.findById(caseRequest.id).orElseThrow().status)
                val result = processingResultRepository.findFirstByCaseRequestIdOrderByRunNumberDesc(caseRequest.id).orElseThrow()
                assertEquals(ProcessingDecision.FALHA_TECNICA, result.decision)
            }
        }
    }

    @Test
    fun `arquivo removido durante analise produz falha tecnica e retry`() {
        caseRequest.title = "Documento removido durante a análise"
        caseRequestRepository.save(caseRequest)
        val content = "%PDF-1.4\nDocumento de teste\n%%EOF".toByteArray()
        val storageKey = "removed-${UUID.randomUUID()}.pdf"
        documentStoragePort.store(storageKey, content)
        val document = CaseDocument(
            caseRequest = caseRequest,
            category = DocumentCategory.IDENTIFICACAO,
            fileName = "identificacao.pdf",
            fileSize = content.size.toLong(),
            contentType = "application/pdf",
            storageKey = storageKey,
            sha256 = PdfDocumentRules.sha256(content),
            uploadState = UploadState.READY
        )
        caseRequest.documents.add(document)
        caseDocumentRepository.save(document)
        documentStoragePort.delete(storageKey)

        val claim = requireNotNull(processingJobClaimService.claimNext())
        analysisEngineService.processClaimedJob(claim.jobId, claim.leaseToken)
        val job = processingJobRepository.findById(claim.jobId).orElseThrow()

        assertEquals(1, job.attemptCount)
        assertEquals(JobState.SCHEDULED, job.state)
        assertEquals(CaseStatus.PROCESSANDO, caseRequestRepository.findById(caseRequest.id).orElseThrow().status)
        assertTrue(job.lastError?.contains("Documento indisponível no armazenamento") == true)
        assertEquals(1, notificationRepository.findByRecipientSubjectOrderByCreatedAtDesc(caseRequest.ownerSubject).size)
        assertTrue(processingResultRepository.findByCaseRequestIdOrderByRunNumberDesc(caseRequest.id).isEmpty())
    }
}
