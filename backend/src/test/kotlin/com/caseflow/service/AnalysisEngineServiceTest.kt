package com.caseflow.service

import com.caseflow.domain.enums.*
import com.caseflow.domain.model.*
import com.caseflow.repository.CaseRequestRepository
import com.caseflow.repository.ProcessingJobRepository
import com.caseflow.repository.ProcessingResultRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AnalysisEngineServiceTest {

    private lateinit var caseRequestRepository: CaseRequestRepository
    private lateinit var processingJobRepository: ProcessingJobRepository
    private lateinit var processingResultRepository: ProcessingResultRepository
    private lateinit var historyService: HistoryService
    private lateinit var notificationService: NotificationService
    private val objectMapper = ObjectMapper()

    private lateinit var analysisEngineService: AnalysisEngineService

    @BeforeEach
    fun setup() {
        caseRequestRepository = mock(CaseRequestRepository::class.java)
        processingJobRepository = mock(ProcessingJobRepository::class.java)
        processingResultRepository = mock(ProcessingResultRepository::class.java)
        historyService = mock(HistoryService::class.java)
        notificationService = mock(NotificationService::class.java)

        `when`(processingResultRepository.save(any(ProcessingResult::class.java))).thenAnswer { it.arguments[0] }
        `when`(caseRequestRepository.save(any(CaseRequest::class.java))).thenAnswer { it.arguments[0] }
        `when`(processingJobRepository.save(any(ProcessingJob::class.java))).thenAnswer { it.arguments[0] }

        analysisEngineService = AnalysisEngineService(
            caseRequestRepository,
            processingJobRepository,
            processingResultRepository,
            historyService,
            notificationService,
            objectMapper
        )
    }

    @Test
    fun `deve aprovar solicitacao quando possui identificacao e comprovante de endereco validos`() {
        val caseRequest = CaseRequest(
            title = "Solicitacao de Teste",
            description = "Descricao de teste com tamanho suficiente para validacao",
            submittedAt = LocalDateTime.now()
        )

        val docId = CaseDocument(
            caseRequest = caseRequest,
            category = DocumentCategory.IDENTIFICACAO,
            validUntil = LocalDate.now().plusYears(1),
            uploadState = UploadState.READY
        )
        val docEnd = CaseDocument(
            caseRequest = caseRequest,
            category = DocumentCategory.COMPROVANTE_ENDERECO,
            validUntil = LocalDate.now().plusYears(1),
            uploadState = UploadState.READY
        )
        caseRequest.documents.addAll(listOf(docId, docEnd))

        val job = ProcessingJob(
            caseRequest = caseRequest,
            runNumber = 1,
            state = JobState.SCHEDULED
        )

        val result = analysisEngineService.executeAnalysis(job)

        assertEquals(ProcessingDecision.APROVADA, result.decision)
        assertEquals(CaseStatus.APROVADA, caseRequest.status)
        assertEquals("[]", result.reasonCodes)
    }

    @Test
    fun `deve rejeitar solicitacao quando falta comprovante de endereco`() {
        val caseRequest = CaseRequest(
            title = "Solicitacao sem comprovante",
            description = "Descricao de teste com tamanho suficiente para validacao",
            submittedAt = LocalDateTime.now()
        )

        val docId = CaseDocument(
            caseRequest = caseRequest,
            category = DocumentCategory.IDENTIFICACAO,
            validUntil = LocalDate.now().plusYears(1),
            uploadState = UploadState.READY
        )
        caseRequest.documents.add(docId)

        val job = ProcessingJob(
            caseRequest = caseRequest,
            runNumber = 1,
            state = JobState.SCHEDULED
        )

        val result = analysisEngineService.executeAnalysis(job)

        assertEquals(ProcessingDecision.REJEITADA, result.decision)
        assertEquals(CaseStatus.REJEITADA, caseRequest.status)
        assertTrue(result.reasonCodes.contains("FALTA_COMPROVANTE_ENDERECO"))
    }

    @Test
    fun `deve rejeitar solicitacao quando documento de identificacao esta vencido`() {
        val caseRequest = CaseRequest(
            title = "Solicitacao com doc vencido",
            description = "Descricao de teste com tamanho suficiente para validacao",
            submittedAt = LocalDateTime.now()
        )

        val docId = CaseDocument(
            caseRequest = caseRequest,
            category = DocumentCategory.IDENTIFICACAO,
            validUntil = LocalDate.now().minusDays(5), // Vencido
            uploadState = UploadState.READY
        )
        val docEnd = CaseDocument(
            caseRequest = caseRequest,
            category = DocumentCategory.COMPROVANTE_ENDERECO,
            validUntil = LocalDate.now().plusYears(1),
            uploadState = UploadState.READY
        )
        caseRequest.documents.addAll(listOf(docId, docEnd))

        val job = ProcessingJob(
            caseRequest = caseRequest,
            runNumber = 1,
            state = JobState.SCHEDULED
        )

        val result = analysisEngineService.executeAnalysis(job)

        assertEquals(ProcessingDecision.REJEITADA, result.decision)
        assertTrue(result.reasonCodes.contains("DOCUMENTO_VENCIDO_IDENTIFICACAO"))
    }
}
