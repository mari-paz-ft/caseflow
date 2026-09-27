package com.caseflow.application.usecase

import com.caseflow.application.dto.UpdateCaseDto
import com.caseflow.application.port.CaseRequestRepository
import com.caseflow.application.port.CaseHistoryRepository
import com.caseflow.application.port.NotificationRepository
import com.caseflow.domain.model.CaseRequest
import com.caseflow.domain.model.Notification
import com.caseflow.infrastructure.persistence.DemoCaseSubjects
import com.caseflow.infrastructure.persistence.LegacySubjectMigration
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CaseAuthorizationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var caseRequestRepository: CaseRequestRepository

    @Autowired
    private lateinit var caseHistoryRepository: CaseHistoryRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var legacySubjectMigration: LegacySubjectMigration

    private lateinit var userASubject: UUID
    private lateinit var caseA: CaseRequest
    private lateinit var caseB: CaseRequest

    @BeforeEach
    fun createCases() {
        userASubject = subjectFor("user-a")
        val userBSubject = subjectFor("user-b")
        caseA = caseRequestRepository.save(newCase(userASubject))
        caseB = caseRequestRepository.save(newCase(userBSubject))
    }

    @Test
    fun `USER A acessa Case A e nao acessa Case B`() {
        val userAToken = token("user-a", "USER")

        mockMvc.perform(get("/api/v1/cases/{id}", caseA.id).header("Authorization", "Bearer $userAToken"))
            .andExpect(status().isOk)
        mockMvc.perform(get("/api/v1/cases/{id}", caseB.id).header("Authorization", "Bearer $userAToken"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `ADMIN acessa Case A e Case B`() {
        val adminToken = token("demo-admin", "ADMIN")

        mockMvc.perform(get("/api/v1/cases/{id}", caseA.id).header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk)
        mockMvc.perform(get("/api/v1/cases/{id}", caseB.id).header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk)
    }

    @Test
    fun `ADMIN nao pode editar rascunho de outro usuario`() {
        val adminToken = token("demo-admin", "ADMIN")
        val update = UpdateCaseDto("Título atualizado", "Descrição válida para atualização do rascunho.", 1)

        mockMvc.perform(
            put("/api/v1/cases/{id}", caseA.id)
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update))
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `edicoes concorrentes com a mesma versao nao sobrescrevem o rascunho`() {
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val updates = listOf("Título concorrente A", "Título concorrente B").map { title ->
                executor.submit<MockHttpServletResponse> {
                    start.await()
                    mockMvc.perform(
                        put("/api/v1/cases/{id}", caseA.id)
                            .header("Authorization", "Bearer ${token("user-a", "USER")}")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(UpdateCaseDto(
                                title = title,
                                description = "Descrição concorrente válida para atualizar o rascunho.",
                                version = 1
                            )))
                    ).andReturn().response
                }
            }
            start.countDown()
            val responses = updates.map { it.get(15, TimeUnit.SECONDS) }

            assertEquals(listOf(200, 409), responses.map { it.status }.sorted())
            val conflict = responses.single { it.status == 409 }
            assertEquals("VERSION_MISMATCH", objectMapper.readTree(conflict.contentAsString).path("errorCode").asText())
            val savedCase = caseRequestRepository.findById(caseA.id).orElseThrow()
            assertTrue(savedCase.title == "Título concorrente A" || savedCase.title == "Título concorrente B")
            assertEquals(2, savedCase.version)
            assertEquals(
                1,
                caseHistoryRepository.findByCaseRequestIdOrderByOccurredAtDesc(caseA.id)
                    .count { it.eventType == "RASCUNHO_ATUALIZADO" }
            )
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `migra os sujeitos UUID legados sem remover casos ou notificacoes`() {
        val legacyUserSubject = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val legacyAdminSubject = UUID.fromString("99999999-9999-9999-9999-999999999999")
        val legacyUserCase = caseRequestRepository.save(newCase(legacyUserSubject))
        val legacyAdminCase = caseRequestRepository.save(newCase(legacyAdminSubject))
        val legacyUserNotification = notificationRepository.save(
            Notification(
                caseRequest = legacyUserCase,
                recipientSubject = legacyUserSubject,
                title = "Teste USER",
                message = "Notificação preservada"
            )
        )
        val legacyAdminNotification = notificationRepository.save(
            Notification(
                caseRequest = legacyAdminCase,
                recipientSubject = legacyAdminSubject,
                title = "Teste ADMIN",
                message = "Notificação preservada"
            )
        )

        legacySubjectMigration.run(DefaultApplicationArguments())
        entityManager.clear()

        val newUserSubject = DemoCaseSubjects.fromUsername(DemoCaseSubjects.USERNAME)
        val newAdminSubject = DemoCaseSubjects.fromUsername(DemoCaseSubjects.ADMIN_USERNAME)
        assertEquals(newUserSubject, caseRequestRepository.findById(legacyUserCase.id).orElseThrow().ownerSubject)
        assertEquals(newAdminSubject, caseRequestRepository.findById(legacyAdminCase.id).orElseThrow().ownerSubject)
        assertEquals(newUserSubject, notificationRepository.findById(legacyUserNotification.id).orElseThrow().recipientSubject)
        assertEquals(newAdminSubject, notificationRepository.findById(legacyAdminNotification.id).orElseThrow().recipientSubject)
    }

    @Test
    fun `requisicao sem JWT valido e rejeitada`() {
        mockMvc.perform(get("/api/v1/cases/{id}", caseA.id))
            .andExpect(status().isUnauthorized)

        val validToken = token("user-a", "USER")
        val tamperedToken = validToken.substringBeforeLast('.') + ".AA"
        mockMvc.perform(get("/api/v1/cases/{id}", caseA.id).header("Authorization", "Bearer $tamperedToken"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `USER lista somente notificacoes proprias`() {
        val ownNotification = notificationRepository.save(
            Notification(caseRequest = caseA, recipientSubject = userASubject, title = "Própria", message = "Mensagem")
        )
        val otherNotification = notificationRepository.save(
            Notification(caseRequest = caseB, recipientSubject = subjectFor("user-b"), title = "Alheia", message = "Mensagem")
        )
        val response = mockMvc.perform(
            get("/api/v1/notifications").header("Authorization", "Bearer ${token("user-a", "USER")}")
        ).andExpect(status().isOk).andReturn().response.contentAsString
        val notificationIds = objectMapper.readTree(response).map { it.path("id").asText() }.toSet()

        assertTrue(ownNotification.id.toString() in notificationIds)
        assertFalse(otherNotification.id.toString() in notificationIds)
    }

    private fun newCase(ownerSubject: UUID) = CaseRequest(
        protocol = "CF-${UUID.randomUUID()}",
        ownerSubject = ownerSubject,
        ownerEmail = ownerSubject.toString(),
        title = "Caso para teste de autorização",
        description = "Descrição utilizada para validar autorização de acesso ao caso.",
    )

    private fun token(username: String, role: String): String {
        val subject = subjectFor(username)
        val now = Instant.now().epochSecond
        val header = encode(objectMapper.writeValueAsBytes(mapOf("alg" to "HS256", "typ" to "JWT")))
        val claims = encode(
            objectMapper.writeValueAsBytes(
                mapOf(
                    "sub" to subject.toString(),
                    "username" to username,
                    "roles" to listOf(role),
                    "iat" to now,
                    "exp" to now + 3600,
                    "iss" to "caseflow-auth-service"
                )
            )
        )
        val signingInput = "$header.$claims"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(TEST_SECRET.toByteArray(UTF_8), "HmacSHA256"))
        val signature = encode(mac.doFinal(signingInput.toByteArray(UTF_8)))
        return "$signingInput.$signature"
    }

    private fun subjectFor(username: String) = UUID.nameUUIDFromBytes(username.toByteArray(UTF_8))

    private fun encode(value: ByteArray) = Base64.getUrlEncoder().withoutPadding().encodeToString(value)

    companion object {
        private const val TEST_SECRET = "caseflow-test-only-secret-value-0123456789"
    }
}
