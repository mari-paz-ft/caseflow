package com.caseflow.bff

import com.caseflow.bff.application.dto.BffCaseResponseDto
import com.caseflow.bff.application.dto.BffSubmitCaseRequest
import com.caseflow.bff.application.exception.DownstreamServiceException
import com.caseflow.bff.application.port.AuthServiceClient
import com.caseflow.bff.application.port.AuthServiceLoginResult
import com.caseflow.bff.application.port.CaseServiceClient
import com.caseflow.bff.application.port.DownstreamRequestHeaders
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant
import java.time.LocalDateTime
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BffIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var authServiceClient: AuthServiceClient

    @MockBean
    private lateinit var caseServiceClient: CaseServiceClient

    @Test
    fun `login encaminha ao auth service e devolve DTO do BFF`() {
        val expiry = Instant.parse("2030-01-01T01:00:00Z")
        `when`(authServiceClient.login("new-user", "password"))
            .thenReturn(AuthServiceLoginResult("jwt-value", "Bearer", expiry, subject("new-user").toString(), "new-user", setOf("USER")))

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"new-user","password":"password"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").value("jwt-value"))
            .andExpect(jsonPath("$.user.id").value(subject("new-user").toString()))
            .andExpect(jsonPath("$.user.role").value("ROLE_USER"))
    }

    @Test
    fun `case requests passam pelo BFF e propagam Authorization Idempotency-Key e Correlation-Id`() {
        val username = "user-a"
        val token = token(username, "USER")
        val caseId = UUID.randomUUID()
        val request = BffSubmitCaseRequest(version = 7)
        val headers = DownstreamRequestHeaders("Bearer $token", "submit-key-7", "correlation-7")
        `when`(caseServiceClient.submitCase(caseId, request, headers)).thenReturn(caseResponse(caseId))

        mockMvc.perform(
            post("/bff/v1/cases/{id}/submit", caseId)
                .header("Authorization", "Bearer $token")
                .header("Idempotency-Key", "submit-key-7")
                .header("Correlation-Id", "correlation-7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"version":7}""")
        ).andExpect(status().isAccepted)

        verify(caseServiceClient).submitCase(caseId, request, headers)
    }

    @Test
    fun `falha downstream e traduzida sem stack trace`() {
        val token = token("user-a", "USER")
        val caseId = UUID.randomUUID()
        `when`(caseServiceClient.getCase(caseId, DownstreamRequestHeaders("Bearer $token")))
            .thenThrow(DownstreamServiceException(503, "CASE_SERVICE_UNAVAILABLE", "case-service está indisponível", "trace-503"))

        mockMvc.perform(get("/bff/v1/cases/{id}", caseId).header("Authorization", "Bearer $token"))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.errorCode").value("CASE_SERVICE_UNAVAILABLE"))
            .andExpect(jsonPath("$.traceId").value("trace-503"))
            .andExpect(jsonPath("$.stackTrace").doesNotExist())
    }

    @Test
    fun `submit pelo BFF exige Idempotency-Key`() {
        val token = token("user-a", "USER")

        mockMvc.perform(
            post("/bff/v1/cases/{id}/submit", UUID.randomUUID())
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"version":1}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_KEY_REQUIRED"))
    }

    @Test
    fun `rota BFF protegida rejeita token ausente ou adulterado`() {
        mockMvc.perform(get("/bff/v1/cases"))
            .andExpect(status().isUnauthorized)

        val validToken = token("user-a", "USER")
        val tamperedToken = validToken.substringBeforeLast('.') + ".AA"
        mockMvc.perform(get("/bff/v1/cases").header("Authorization", "Bearer $tamperedToken"))
            .andExpect(status().isUnauthorized)
    }

    private fun caseResponse(id: UUID) = BffCaseResponseDto(
        id = id.toString(),
        protocol = "CF-test",
        ownerSubject = subject("user-a").toString(),
        ownerEmail = "user-a",
        title = "Caso para teste",
        description = "Descrição para teste da integração BFF.",
        caseType = "ANALISE_DOCUMENTAL",
        status = "ENVIADA",
        version = 8,
        processingRun = 1,
        rulesVersion = "DOCUMENTAL_V1",
        submittedAt = LocalDateTime.now(),
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        documents = emptyList(),
        latestResult = null
    )

    private fun token(username: String, role: String): String {
        val now = Instant.now().epochSecond
        val header = encode(objectMapper.writeValueAsBytes(mapOf("alg" to "HS256", "typ" to "JWT")))
        val claims = encode(
            objectMapper.writeValueAsBytes(
                mapOf(
                    "sub" to subject(username).toString(),
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
        return "$signingInput.${encode(mac.doFinal(signingInput.toByteArray(UTF_8)))}"
    }

    private fun subject(username: String) = UUID.nameUUIDFromBytes(username.toByteArray(UTF_8))

    private fun encode(value: ByteArray) = Base64.getUrlEncoder().withoutPadding().encodeToString(value)

    companion object {
        private const val TEST_SECRET = "caseflow-test-only-secret-value-0123456789"
    }
}
