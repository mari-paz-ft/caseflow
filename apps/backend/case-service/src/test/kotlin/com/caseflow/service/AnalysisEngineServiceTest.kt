package com.caseflow.application.usecase

import com.caseflow.application.port.CaseRequestRepository
import com.caseflow.application.port.DocumentStoragePort
import com.caseflow.application.port.ProcessingJobRepository
import com.caseflow.application.port.ProcessingResultRepository
import com.caseflow.domain.enums.CaseStatus
import com.caseflow.domain.enums.DocumentCategory
import com.caseflow.domain.enums.JobState
import com.caseflow.domain.enums.ProcessingDecision
import com.caseflow.domain.enums.UploadState
import com.caseflow.domain.exception.TechnicalFailureException
import com.caseflow.domain.model.CaseDocument
import com.caseflow.domain.model.CaseRequest
import com.caseflow.domain.model.ProcessingJob
import com.caseflow.domain.model.ProcessingResult
import com.caseflow.domain.rule.PdfDocumentRules
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AnalysisEngineServiceTest {
    private lateinit var caseRequestRepository: CaseRequestRepository
    private lateinit var processingJobRepository: ProcessingJobRepository
    private lateinit var processingResultRepository: ProcessingResultRepository
    private lateinit var documentStoragePort: DocumentStoragePort
    private lateinit var historyService: HistoryService
    private lateinit var notificationService: NotificationService
    private val objectMapper = ObjectMapper()
    private val testPdf = "%PDF-1.4\nCaseFlow test document".toByteArray()

    private lateinit var analysisEngineService: AnalysisEngineService

    @BeforeEach
    fun setup() {
        processingResultRepository = saveEchoingRepository(ProcessingResultRepository::class.java)
        documentStoragePort = Mockito.mock(DocumentStoragePort::class.java)
        historyService = Mockito.mock(HistoryService::class.java)
        notificationService = Mockito.mock(NotificationService::class.java)

        Mockito.`when`(documentStoragePort.read(Mockito.anyString())).thenReturn(testPdf)
        caseRequestRepository = saveEchoingRepository(CaseRequestRepository::class.java)
        processingJobRepository = saveEchoingRepository(ProcessingJobRepository::class.java)

        analysisEngineService = AnalysisEngineService(
            caseRequestRepository,
            processingJobRepository,
            processingResultRepository,
            historyService,
            notificationService,
            objectMapper,
            documentStoragePort
        )
    }

    @Test
    fun `deve aprovar solicitacao quando possui identificacao e comprovante de endereco validos`() {
        val caseRequest = CaseRequest(
            protocol = "CF-APPROVED",
            title = "Solicitacao de Teste",
            description = "Descricao de teste com tamanho suficiente para validacao",
            submittedAt = LocalDateTime.now()
        )
        caseRequest.documents.addAll(
            listOf(
                document(caseRequest, DocumentCategory.IDENTIFICACAO, LocalDate.now().plusYears(1)),
                document(caseRequest, DocumentCategory.COMPROVANTE_ENDERECO, LocalDate.now().plusYears(1))
            )
        )
        val job = ProcessingJob(caseRequest = caseRequest, runNumber = 1, state = JobState.SCHEDULED)

        val result = analysisEngineService.executeAnalysis(job)

        assertEquals(ProcessingDecision.APROVADA, result.decision)
        assertEquals(CaseStatus.APROVADA, caseRequest.status)
        assertEquals("[]", result.reasonCodes)
        Mockito.verify(notificationService).notify(
            caseRequest,
            caseRequest.ownerSubject,
            "Solicitação ${caseRequest.protocol} Aprovada",
            "Sua documentação foi conferida com sucesso e aprovada pelo motor de regras."
        )
        Mockito.verifyNoMoreInteractions(notificationService)
    }

    @Test
    fun `deve rejeitar solicitacao quando falta comprovante de endereco sem retry tecnico`() {
        val caseRequest = CaseRequest(
            protocol = "CF-REJECTED",
            title = "Solicitacao sem comprovante",
            description = "Descricao de teste com tamanho suficiente para validacao",
            submittedAt = LocalDateTime.now()
        )
        caseRequest.documents.add(document(caseRequest, DocumentCategory.IDENTIFICACAO, LocalDate.now().plusYears(1)))
        val job = ProcessingJob(caseRequest = caseRequest, runNumber = 1, state = JobState.SCHEDULED)

        val result = analysisEngineService.executeAnalysis(job)

        assertEquals(ProcessingDecision.REJEITADA, result.decision)
        assertEquals(CaseStatus.REJEITADA, caseRequest.status)
        assertEquals(JobState.COMPLETED, job.state)
        assertEquals(0, job.attemptCount)
        assertTrue(result.reasonCodes.contains("FALTA_COMPROVANTE_ENDERECO"))
        Mockito.verify(notificationService).notify(
            caseRequest,
            caseRequest.ownerSubject,
            "Solicitação ${caseRequest.protocol} Rejeitada",
            "Sua solicitação foi rejeitada pelos seguintes motivos: FALTA_COMPROVANTE_ENDERECO."
        )
        Mockito.verifyNoMoreInteractions(notificationService)
    }

    @Test
    fun `deve rejeitar solicitacao quando documento de identificacao esta vencido`() {
        val caseRequest = CaseRequest(
            title = "Solicitacao com doc vencido",
            description = "Descricao de teste com tamanho suficiente para validacao",
            submittedAt = LocalDateTime.now()
        )
        caseRequest.documents.addAll(
            listOf(
                document(caseRequest, DocumentCategory.IDENTIFICACAO, LocalDate.now().minusDays(5)),
                document(caseRequest, DocumentCategory.COMPROVANTE_ENDERECO, LocalDate.now().plusYears(1))
            )
        )
        val job = ProcessingJob(caseRequest = caseRequest, runNumber = 1, state = JobState.SCHEDULED)

        val result = analysisEngineService.executeAnalysis(job)

        assertEquals(ProcessingDecision.REJEITADA, result.decision)
        assertTrue(result.reasonCodes.contains("DOCUMENTO_VENCIDO_IDENTIFICACAO"))
    }

    @Test
    fun `analysis treats stored PDF size mismatch as technical failure`() {
        assertCorruptPdfRejected(fileSize = testPdf.size.toLong() + 1)
    }

    @Test
    fun `analysis treats stored PDF hash mismatch as technical failure`() {
        assertCorruptPdfRejected(sha256 = "0".repeat(64))
    }

    private fun assertCorruptPdfRejected(
        fileSize: Long = testPdf.size.toLong(),
        sha256: String = PdfDocumentRules.sha256(testPdf)
    ) {
        val caseRequest = CaseRequest(
            title = "Solicitação com arquivo divergente",
            description = "Descrição de teste com tamanho suficiente para validar falha técnica.",
            submittedAt = LocalDateTime.now()
        )
        caseRequest.documents.add(
            CaseDocument(
                caseRequest = caseRequest,
                category = DocumentCategory.IDENTIFICACAO,
                fileName = "document.pdf",
                fileSize = fileSize,
                contentType = "application/pdf",
                storageKey = "document.pdf",
                sha256 = sha256,
                uploadState = UploadState.READY
            )
        )
        val job = ProcessingJob(caseRequest = caseRequest, runNumber = 1, state = JobState.SCHEDULED)

        org.junit.jupiter.api.Assertions.assertThrows(TechnicalFailureException::class.java) {
            analysisEngineService.executeAnalysis(job)
        }
    }

    private fun <T> saveEchoingRepository(type: Class<T>): T = Mockito.mock(
        type,
        Mockito.withSettings().defaultAnswer { invocation ->
            if (invocation.method.name == "save" || invocation.method.name == "saveAndFlush") invocation.arguments[0] else null
        }
    )

    private fun document(caseRequest: CaseRequest, category: DocumentCategory, validUntil: LocalDate) = CaseDocument(
        caseRequest = caseRequest,
        category = category,
        fileName = "document.pdf",
        fileSize = testPdf.size.toLong(),
        contentType = "application/pdf",
        storageKey = "${UUID.randomUUID()}.pdf",
        sha256 = PdfDocumentRules.sha256(testPdf),
        validUntil = validUntil,
        uploadState = UploadState.READY
    )
}
