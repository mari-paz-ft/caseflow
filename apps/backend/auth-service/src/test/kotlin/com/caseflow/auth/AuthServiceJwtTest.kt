package com.caseflow.auth

import com.caseflow.auth.domain.model.AuthRole
import com.caseflow.auth.infrastructure.security.HmacJwtTokenService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthServiceJwtTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `aceita credenciais nao vazias e emite JWT real para USER`() {
        val token = login("sem-conta-persistida", "qualquer-senha")
        val header = objectMapper.readTree(java.util.Base64.getUrlDecoder().decode(token.split('.')[0]))
        val payload = decodePayload(token)

        assertEquals("HS256", header.path("alg").asText())
        assertEquals(UUID.nameUUIDFromBytes("sem-conta-persistida".toByteArray(UTF_8)).toString(), payload.path("sub").asText())
        assertEquals("sem-conta-persistida", payload.path("username").asText())
        assertEquals("USER", payload.path("roles").first().asText())
        assertEquals("caseflow-auth-service", payload.path("iss").asText())
        assertTrue(payload.path("iat").isIntegralNumber)
        assertTrue(payload.path("exp").asLong() > payload.path("iat").asLong())

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
    }

    @Test
    fun `username contendo admin recebe role ADMIN`() {
        val token = login("demo-admin-user", "senha-qualquer")

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect { result ->
                val roles = objectMapper.readTree(result.response.contentAsString).path("roles")
                assertEquals("ADMIN", roles.first().asText())
            }
    }

    @Test
    fun `credenciais vazias sao rejeitadas`() {
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"username":"","password":"senha"}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `token ausente e negado em rota protegida`() {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `token adulterado e rejeitado`() {
        val token = login("usuario", "senha")
        val signatureStart = token.lastIndexOf('.') + 1
        val signature = token.substring(signatureStart)
        val replacement = if (signature.first() == 'A') 'B' else 'A'
        val tamperedToken = token.substring(0, signatureStart) + replacement + signature.drop(1)

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer $tamperedToken"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `token expirado e rejeitado`() {
        val expiredTokenService = HmacJwtTokenService(
            secret = TEST_SECRET,
            ttlSeconds = 60,
            objectMapper = objectMapper,
            clock = Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC)
        )
        val expiredToken = expiredTokenService.issue(
            UUID.nameUUIDFromBytes("usuario".toByteArray(UTF_8)).toString(),
            "usuario",
            setOf(AuthRole.USER)
        ).value

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer $expiredToken"))
            .andExpect(status().isUnauthorized)
    }

    private fun login(username: String, password: String): String {
        val response = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("username" to username, "password" to password)))
        ).andExpect(status().isOk).andReturn().response.contentAsString

        return objectMapper.readTree(response).path("accessToken").asText()
    }

    private fun decodePayload(token: String) =
        objectMapper.readTree(java.util.Base64.getUrlDecoder().decode(token.split('.')[1]))

    companion object {
        private const val TEST_SECRET = "caseflow-test-only-secret-value-0123456789"
    }
}
