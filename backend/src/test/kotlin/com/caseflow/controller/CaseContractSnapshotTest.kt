package com.caseflow.controller

import com.caseflow.config.CurrentUserContext
import com.caseflow.controller.dto.CaseResponseDto
import com.caseflow.controller.dto.LoginRequestDto
import com.caseflow.domain.enums.CaseStatus
import com.caseflow.domain.enums.RoleName
import com.caseflow.domain.model.AppUser
import com.caseflow.domain.model.CaseHistory
import com.caseflow.domain.model.CaseRequest
import com.caseflow.domain.model.Notification
import com.caseflow.service.AuthService
import com.caseflow.service.CaseService
import com.caseflow.service.DocumentService
import com.caseflow.service.HistoryService
import com.caseflow.service.NotificationService
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime
import java.util.UUID

@DisplayName("Testes de Snapshot de Contrato HTTP — CaseController")
class CaseContractSnapshotTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var caseService: CaseService
    private lateinit var documentService: DocumentService
    private lateinit var historyService: HistoryService
    private lateinit var notificationService: NotificationService
    private lateinit var authService: AuthService
    private val objectMapper = ObjectMapper()

    private val userSubject = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val solicitanteUser = AppUser(
        id = userSubject,
        email = "solicitante@caseflow.local",
        fullName = "Carlos Silva (Solicitante)",
        passwordHash = "senha123",
        role = RoleName.ROLE_USER
    )

    @BeforeEach
    fun setup() {
        caseService = mock(CaseService::class.java)
        documentService = mock(DocumentService::class.java)
        historyService = mock(HistoryService::class.java)
        notificationService = mock(NotificationService::class.java)
        authService = mock(AuthService::class.java)
        `when`(authService.getCurrentUser()).thenReturn(solicitanteUser)

        val controller = CaseController(
            caseService,
            documentService,
            historyService,
            notificationService,
            authService
        )

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
        CurrentUserContext.set(solicitanteUser)
    }

    @AfterEach
    fun tearDown() {
        CurrentUserContext.clear()
    }

    @Test
    fun `snapshot GET bff v1 cases id history - deve preservar formato exato do contrato de historico`() {
        val caseId = UUID.fromString("a1111111-0000-0000-0000-000000000001")
        val historyId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        val fixedDateTime = LocalDateTime.of(2026, 9, 22, 10, 30, 0)

        val caseRequest = CaseRequest(
            id = caseId,
            protocol = "CF-20260922-1001",
            ownerSubject = userSubject,
            ownerEmail = solicitanteUser.email,
            title = "Cadastro de Fornecedor",
            description = "Descrição do caso",
            status = CaseStatus.RASCUNHO
        )

        val dummyCaseResponse = CaseResponseDto(
            id = caseId,
            protocol = "CF-20260922-1001",
            ownerSubject = userSubject,
            ownerEmail = solicitanteUser.email,
            title = "Cadastro de Fornecedor",
            description = "Descrição do caso",
            caseType = "ANALISE_DOCUMENTAL",
            status = CaseStatus.RASCUNHO,
            version = 1L,
            processingRun = 0,
            rulesVersion = "DOCUMENTAL_V1",
            submittedAt = null,
            createdAt = fixedDateTime,
            updatedAt = fixedDateTime
        )

        `when`(caseService.getCaseById(eq(caseId), any(AppUser::class.java)))
            .thenReturn(dummyCaseResponse)

        val historyItem = CaseHistory(
            id = historyId,
            caseRequest = caseRequest,
            eventType = "CRIACAO_RASCUNHO",
            actorSubject = solicitanteUser.email,
            details = "Rascunho criado no portal",
            occurredAt = fixedDateTime
        )

        `when`(historyService.getHistoryForCase(caseId)).thenReturn(listOf(historyItem))

        mockMvc.perform(get("/bff/v1/cases/$caseId/history"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].id", `is`(historyId.toString())))
            .andExpect(jsonPath("$[0].caseId", `is`(caseId.toString())))
            .andExpect(jsonPath("$[0].eventType", `is`("CRIACAO_RASCUNHO")))
            .andExpect(jsonPath("$[0].actorSubject", `is`("solicitante@caseflow.local")))
            .andExpect(jsonPath("$[0].details", `is`("Rascunho criado no portal")))
            .andExpect(jsonPath("$[0].occurredAt", notNullValue()))
    }

    @Test
    fun `snapshot GET bff v1 notifications - deve preservar contrato de lista de notificacoes`() {
        val notifId = UUID.fromString("223e4567-e89b-12d3-a456-426614174001")
        val caseId = UUID.fromString("a2222222-0000-0000-0000-000000000002")
        val fixedDateTime = LocalDateTime.of(2026, 9, 22, 14, 0, 0)

        val caseRequest = CaseRequest(
            id = caseId,
            protocol = "CF-20260922-1002",
            ownerSubject = userSubject,
            ownerEmail = solicitanteUser.email,
            title = "Caso Validado",
            description = "Descrição",
            status = CaseStatus.APROVADA
        )

        val notification = Notification(
            id = notifId,
            caseRequest = caseRequest,
            recipientSubject = userSubject,
            title = "Solicitação Aprovada",
            message = "Sua documentação foi aprovada com sucesso.",
            readAt = null,
            createdAt = fixedDateTime
        )

        `when`(notificationService.getNotificationsForUser(userSubject)).thenReturn(listOf(notification))

        mockMvc.perform(get("/bff/v1/notifications"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].id", `is`(notifId.toString())))
            .andExpect(jsonPath("$[0].caseId", `is`(caseId.toString())))
            .andExpect(jsonPath("$[0].title", `is`("Solicitação Aprovada")))
            .andExpect(jsonPath("$[0].message", `is`("Sua documentação foi aprovada com sucesso.")))
            .andExpect(jsonPath("$[0].readAt").doesNotExist())
            .andExpect(jsonPath("$[0].createdAt", notNullValue()))
    }

    @Test
    fun `snapshot POST bff v1 auth login - deve preservar contrato do token e dados do usuario`() {
        val loginPayload = LoginRequestDto(
            email = "solicitante@caseflow.local",
            password = "senha123"
        )
        val loginResponse = com.caseflow.controller.dto.LoginResponseDto(
            "mock-token-solicitante@caseflow.local",
            com.caseflow.controller.dto.UserDto(
                userSubject,
                solicitanteUser.email,
                solicitanteUser.fullName,
                solicitanteUser.role
            )
        )
        `when`(authService.login(loginPayload)).thenReturn(loginResponse)

        mockMvc.perform(
            post("/bff/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginPayload))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.token", `is`("mock-token-solicitante@caseflow.local")))
            .andExpect(jsonPath("$.user.id", `is`(userSubject.toString())))
            .andExpect(jsonPath("$.user.email", `is`("solicitante@caseflow.local")))
            .andExpect(jsonPath("$.user.fullName", `is`("Carlos Silva (Solicitante)")))
            .andExpect(jsonPath("$.user.role", `is`("ROLE_USER")))
    }
}
