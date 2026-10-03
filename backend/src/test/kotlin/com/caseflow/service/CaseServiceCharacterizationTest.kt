package com.caseflow.service

import com.caseflow.controller.dto.CreateCaseDto
import com.caseflow.controller.dto.RetryCaseDto
import com.caseflow.controller.dto.SubmitCaseDto
import com.caseflow.controller.dto.UpdateCaseDto
import com.caseflow.domain.enums.CaseStatus
import com.caseflow.domain.enums.DocumentCategory
import com.caseflow.domain.enums.RoleName
import com.caseflow.domain.enums.UploadState
import com.caseflow.domain.exception.ConflictException
import com.caseflow.domain.exception.ForbiddenException
import com.caseflow.domain.model.AppUser
import com.caseflow.domain.model.CaseDocument
import com.caseflow.domain.model.CaseRequest
import com.caseflow.domain.model.ProcessingJob
import com.caseflow.repository.CaseRequestRepository
import com.caseflow.repository.ProcessingJobRepository
import com.caseflow.repository.ProcessingResultRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

@DisplayName("Testes de Caracterização — CaseService")
class CaseServiceCharacterizationTest {

    private lateinit var caseRequestRepository: CaseRequestRepository
    private lateinit var processingJobRepository: ProcessingJobRepository
    private lateinit var processingResultRepository: ProcessingResultRepository
    private lateinit var analysisEngineService: AnalysisEngineService
    private lateinit var historyService: HistoryService
    private val objectMapper = ObjectMapper()

    private lateinit var caseService: CaseService

    private val userOwner = AppUser(
        id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
        email = "solicitante@caseflow.local",
        fullName = "Carlos Silva",
        passwordHash = "hash",
        role = RoleName.ROLE_USER
    )

    private val userOther = AppUser(
        id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
        email = "outro@caseflow.local",
        fullName = "Outro Solicitante",
        passwordHash = "hash",
        role = RoleName.ROLE_USER
    )

    private val userAdmin = AppUser(
        id = UUID.fromString("99999999-9999-9999-9999-999999999999"),
        email = "admin@caseflow.local",
        fullName = "Mariana Paz",
        passwordHash = "hash",
        role = RoleName.ROLE_ADMIN
    )

    @BeforeEach
    fun setup() {
        caseRequestRepository = mock(CaseRequestRepository::class.java)
        processingJobRepository = mock(ProcessingJobRepository::class.java)
        processingResultRepository = mock(ProcessingResultRepository::class.java)
        analysisEngineService = mock(AnalysisEngineService::class.java)
        historyService = mock(HistoryService::class.java)

        `when`(caseRequestRepository.save(any(CaseRequest::class.java))).thenAnswer { it.arguments[0] }
        `when`(processingJobRepository.save(any(ProcessingJob::class.java))).thenAnswer { it.arguments[0] }
        `when`(processingResultRepository.findFirstByCaseRequestIdOrderByRunNumberDesc(any(UUID::class.java)))
            .thenReturn(Optional.empty())

        caseService = CaseService(
            caseRequestRepository,
            processingJobRepository,
            processingResultRepository,
            analysisEngineService,
            historyService,
            objectMapper
        )
    }

    @Nested
    @DisplayName("Criação de Caso (createCase)")
    inner class CreateCaseTests {

        @Test
        fun `deve criar caso em status RASCUNHO com versao inicial 1 e protocolo formatado`() {
            val dto = CreateCaseDto(
                title = "Nova Solicitação de Teste",
                description = "Descrição detalhada com tamanho adequado para validação",
                type = "ANALISE_DOCUMENTAL"
            )

            val result = caseService.createCase(dto, userOwner)

            assertNotNull(result.id)
            assertTrue(result.protocol.startsWith("CF-"))
            assertEquals(CaseStatus.RASCUNHO, result.status)
            assertEquals(1L, result.version)
            assertEquals(0, result.processingRun)
            assertEquals(userOwner.id, result.ownerSubject)
            assertEquals(userOwner.email, result.ownerEmail)
            assertEquals("Nova Solicitação de Teste", result.title)

            verify(historyService, times(1)).record(
                caseRequest = any(CaseRequest::class.java),
                eventType = eq("CRIACAO_RASCUNHO"),
                actorSubject = eq(userOwner.email),
                details = anyString()
            )
        }
    }

    @Nested
    @DisplayName("Envio para Análise (submitCase)")
    inner class SubmitCaseTests {

        @Test
        fun `caminho feliz - deve transitar para ENVIADA agendar job e chamar motor assincrono`() {
            val caseId = UUID.randomUUID()
            val existingCase = CaseRequest(
                id = caseId,
                protocol = "CF-20260922-1001",
                ownerSubject = userOwner.id,
                ownerEmail = userOwner.email,
                title = "Solicitação Completa",
                description = "Descrição válida da solicitação",
                status = CaseStatus.RASCUNHO,
                version = 1L
            )
            val doc = CaseDocument(
                caseRequest = existingCase,
                category = DocumentCategory.IDENTIFICACAO,
                validUntil = LocalDate.now().plusYears(1),
                uploadState = UploadState.READY
            )
            existingCase.documents.add(doc)

            `when`(caseRequestRepository.findById(caseId)).thenReturn(Optional.of(existingCase))

            val submitDto = SubmitCaseDto(version = 1L)
            val result = caseService.submitCase(caseId, submitDto, userOwner, idempotencyKey = "key-123")

            assertEquals(CaseStatus.ENVIADA, result.status)
            assertEquals(2L, result.version)
            assertEquals(1, result.processingRun)
            assertNotNull(result.submittedAt)

            verify(processingJobRepository, times(1)).save(any(ProcessingJob::class.java))
            verify(analysisEngineService, times(1)).processJobAsync(any(ProcessingJob::class.java))
            verify(historyService, times(1)).record(
                caseRequest = eq(existingCase),
                eventType = eq("SOLICITACAO_ENVIADA"),
                actorSubject = eq(userOwner.email),
                details = anyString()
            )
        }

        @Test
        fun `idempotencia - deve retornar caso inalterado se ja foi enviado anteriormente`() {
            val caseId = UUID.randomUUID()
            val submittedCase = CaseRequest(
                id = caseId,
                protocol = "CF-20260922-1001",
                ownerSubject = userOwner.id,
                ownerEmail = userOwner.email,
                title = "Solicitação Já Enviada",
                description = "Descrição da solicitação",
                status = CaseStatus.ENVIADA,
                version = 2L,
                processingRun = 1
            )

            `when`(caseRequestRepository.findById(caseId)).thenReturn(Optional.of(submittedCase))

            val submitDto = SubmitCaseDto(version = 1L)
            val result = caseService.submitCase(caseId, submitDto, userOwner, idempotencyKey = "key-repeat")

            assertEquals(CaseStatus.ENVIADA, result.status)
            assertEquals(2L, result.version)
            verify(analysisEngineService, never()).processJobAsync(any(ProcessingJob::class.java))
        }

        @Test
        fun `deve falhar se usuario nao for o autor da solicitacao`() {
            val caseId = UUID.randomUUID()
            val existingCase = CaseRequest(
                id = caseId,
                ownerSubject = userOwner.id,
                status = CaseStatus.RASCUNHO
            )
            `when`(caseRequestRepository.findById(caseId)).thenReturn(Optional.of(existingCase))

            val submitDto = SubmitCaseDto(version = 1L)
            assertThrows(ForbiddenException::class.java) {
                caseService.submitCase(caseId, submitDto, userOther, null)
            }
        }

        @Test
        fun `deve falhar se houver conflito de versao concorrente`() {
            val caseId = UUID.randomUUID()
            val existingCase = CaseRequest(
                id = caseId,
                ownerSubject = userOwner.id,
                status = CaseStatus.RASCUNHO,
                version = 2L
            )
            `when`(caseRequestRepository.findById(caseId)).thenReturn(Optional.of(existingCase))

            val submitDto = SubmitCaseDto(version = 1L) // versão enviada defasada
            val ex = assertThrows(ConflictException::class.java) {
                caseService.submitCase(caseId, submitDto, userOwner, null)
            }
            assertEquals("VERSION_MISMATCH", ex.errorCode)
        }

        @Test
        fun `deve falhar se nao houver nenhum documento anexado`() {
            val caseId = UUID.randomUUID()
            val existingCase = CaseRequest(
                id = caseId,
                ownerSubject = userOwner.id,
                status = CaseStatus.RASCUNHO,
                version = 1L
            )
            `when`(caseRequestRepository.findById(caseId)).thenReturn(Optional.of(existingCase))

            val submitDto = SubmitCaseDto(version = 1L)
            val ex = assertThrows(ConflictException::class.java) {
                caseService.submitCase(caseId, submitDto, userOwner, null)
            }
            assertEquals("NO_DOCUMENTS_ATTACHED", ex.errorCode)
        }
    }

    @Nested
    @DisplayName("Reprocessamento de Falha Técnica (retryCase)")
    inner class RetryCaseTests {

        @Test
        fun `administrador deve conseguir reprocessar caso em FALHA_TECNICA com justificativa`() {
            val caseId = UUID.randomUUID()
            val caseInFailure = CaseRequest(
                id = caseId,
                protocol = "CF-20260922-1004",
                ownerSubject = userOwner.id,
                ownerEmail = userOwner.email,
                title = "Caso em Falha Técnica",
                description = "Descrição válida",
                status = CaseStatus.FALHA_TECNICA,
                version = 3L,
                processingRun = 1
            )
            `when`(caseRequestRepository.findById(caseId)).thenReturn(Optional.of(caseInFailure))

            val retryDto = RetryCaseDto(justification = "Disco restaurado pelo time de infraestrutura.")
            val result = caseService.retryCase(caseId, retryDto, userAdmin, "idemp-key")

            assertEquals(CaseStatus.ENVIADA, result.status)
            assertEquals(4L, result.version)
            assertEquals(2, result.processingRun)

            verify(processingJobRepository, times(1)).save(any(ProcessingJob::class.java))
            verify(analysisEngineService, times(1)).processJobAsync(any(ProcessingJob::class.java))
            verify(historyService, times(1)).record(
                caseRequest = eq(caseInFailure),
                eventType = eq("REPROCESSAMENTO_SOLICITADO"),
                actorSubject = eq(userAdmin.email),
                details = contains("Disco restaurado")
            )
        }

        @Test
        fun `solicitante comum deve ser bloqueado com ForbiddenException ao tentar retry`() {
            val caseId = UUID.randomUUID()
            val retryDto = RetryCaseDto(justification = "Tentativa de solicitante comum")

            assertThrows(ForbiddenException::class.java) {
                caseService.retryCase(caseId, retryDto, userOwner, null)
            }
            verify(caseRequestRepository, never()).findById(any(UUID::class.java))
        }

        @Test
        fun `administrador nao pode reprocessar caso que nao esteja em FALHA_TECNICA`() {
            val caseId = UUID.randomUUID()
            val caseApproved = CaseRequest(
                id = caseId,
                ownerSubject = userOwner.id,
                status = CaseStatus.APROVADA
            )
            `when`(caseRequestRepository.findById(caseId)).thenReturn(Optional.of(caseApproved))

            val retryDto = RetryCaseDto(justification = "Reprocessar caso ja aprovado")
            val ex = assertThrows(ConflictException::class.java) {
                caseService.retryCase(caseId, retryDto, userAdmin, null)
            }
            assertEquals("INVALID_STATE_FOR_RETRY", ex.errorCode)
        }
    }

    @Nested
    @DisplayName("Edição de Rascunho (updateCase)")
    inner class UpdateCaseTests {

        @Test
        fun `deve atualizar titulo e descricao incrementando a versao`() {
            val caseId = UUID.randomUUID()
            val existingCase = CaseRequest(
                id = caseId,
                ownerSubject = userOwner.id,
                ownerEmail = userOwner.email,
                title = "Título Antigo",
                description = "Descrição antiga válida",
                status = CaseStatus.RASCUNHO,
                version = 1L
            )
            `when`(caseRequestRepository.findById(caseId)).thenReturn(Optional.of(existingCase))

            val updateDto = UpdateCaseDto(
                title = "Título Atualizado",
                description = "Nova descrição atualizada",
                version = 1L
            )

            val result = caseService.updateCase(caseId, updateDto, userOwner)

            assertEquals("Título Atualizado", result.title)
            assertEquals("Nova descrição atualizada", result.description)
            assertEquals(2L, result.version)

            verify(historyService, times(1)).record(
                caseRequest = eq(existingCase),
                eventType = eq("RASCUNHO_ATUALIZADO"),
                actorSubject = eq(userOwner.email),
                details = anyString()
            )
        }

        @Test
        fun `deve lancar ConflictException se tentar editar caso que nao e RASCUNHO`() {
            val caseId = UUID.randomUUID()
            val immutableCase = CaseRequest(
                id = caseId,
                ownerSubject = userOwner.id,
                status = CaseStatus.APROVADA,
                version = 2L
            )
            `when`(caseRequestRepository.findById(caseId)).thenReturn(Optional.of(immutableCase))

            val updateDto = UpdateCaseDto(
                title = "Tentativa de Edição",
                description = "Não deve ser permitido alterar caso aprovado",
                version = 2L
            )

            val ex = assertThrows(ConflictException::class.java) {
                caseService.updateCase(caseId, updateDto, userOwner)
            }
            assertEquals("CASE_IMMUTABLE", ex.errorCode)
        }

        @Test
        fun `deve lancar ConflictException se versao informada nao for igual a versao atual`() {
            val caseId = UUID.randomUUID()
            val existingCase = CaseRequest(
                id = caseId,
                ownerSubject = userOwner.id,
                status = CaseStatus.RASCUNHO,
                version = 3L
            )
            `when`(caseRequestRepository.findById(caseId)).thenReturn(Optional.of(existingCase))

            val updateDto = UpdateCaseDto(
                title = "Conflito de Versão",
                description = "Descrição de teste com conflito",
                version = 2L // esperava 3L
            )

            val ex = assertThrows(ConflictException::class.java) {
                caseService.updateCase(caseId, updateDto, userOwner)
            }
            assertEquals("VERSION_MISMATCH", ex.errorCode)
        }
    }
}
