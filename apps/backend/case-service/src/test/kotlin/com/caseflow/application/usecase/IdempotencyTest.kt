package com.caseflow.application.usecase

import com.caseflow.application.port.CaseDocumentRepository
import com.caseflow.application.port.CaseHistoryRepository
import com.caseflow.application.port.CaseRequestRepository
import com.caseflow.application.port.IdempotencyRecordRepository
import com.caseflow.application.port.ProcessingJobRepository
import com.caseflow.application.port.NotificationRepository
import com.caseflow.domain.enums.CaseStatus
import com.caseflow.domain.enums.DocumentCategory
import com.caseflow.domain.enums.UploadState
import com.caseflow.domain.model.CaseDocument
import com.caseflow.domain.model.CaseRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IdempotencyTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var caseRequestRepository: CaseRequestRepository

    @Autowired
    private lateinit var caseDocumentRepository: CaseDocumentRepository

    @Autowired
    private lateinit var caseHistoryRepository: CaseHistoryRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var processingJobRepository: ProcessingJobRepository

    @Autowired
    private lateinit var idempotencyRecordRepository: IdempotencyRecordRepository

    @MockBean
    private lateinit var analysisEngineService: AnalysisEngineService

    private lateinit var ownerSubject: UUID
    private lateinit var userToken: String
    private lateinit var draftCase: CaseRequest

    @BeforeEach
    fun setUp() {
        ownerSubject = subject(USERNAME)
        userToken = token(USERNAME, "USER")
        draftCase = caseRequestRepository.save(
            CaseRequest(
                protocol = "CF-IDEMP-${UUID.randomUUID()}",
                ownerSubject = ownerSubject,
                ownerEmail = USERNAME,
                title = "Solicitação idempotente",
                description = "Descrição de caso usada para verificar idempotência do envio.",
                status = CaseStatus.RASCUNHO,
                version = 1
            )
        )
        val document = CaseDocument(
            caseRequest = draftCase,
            category = DocumentCategory.IDENTIFICACAO,
            fileName = "identificacao.pdf",
            fileSize = 128,
            contentType = "application/pdf",
            storageKey = "${draftCase.id}-identificacao.pdf",
            sha256 = "a".repeat(64),
            uploadState = UploadState.READY
        )
        caseDocumentRepository.save(document)
    }

    @Test
    fun `submit repetido com mesma chave e contexto reutiliza resposta e job`() {
        val key = "submit-${UUID.randomUUID()}"
        val firstResponse = submit(draftCase.id, 1, key)
            .andExpect(status().isAccepted)
            .andReturn().response.contentAsString
        val replayResponse = submit(draftCase.id, 1, key)
            .andExpect(status().isAccepted)
            .andReturn().response.contentAsString

        assertEquals(
            objectMapper.readTree(firstResponse).path("version").asLong(),
            objectMapper.readTree(replayResponse).path("version").asLong()
        )
        assertEquals(1, processingJobRepository.findByCaseRequestId(draftCase.id).size)
        assertEquals(1, idempotencyRecordRepository.countByOperationAndIdempotencyKey("SUBMIT", key))
        assertEquals(1, caseHistoryRepository.findByCaseRequestIdOrderByOccurredAtDesc(draftCase.id).size)
        assertTrue(notificationRepository.findByRecipientSubjectOrderByCreatedAtDesc(ownerSubject).isEmpty())
    }

    @Test
    fun `submit concorrente com mesma chave e contexto cria um job e uma transicao`() {
        val key = "submit-concurrent-${UUID.randomUUID()}"
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val responses = (1..2).map {
                executor.submit<MockHttpServletResponse> {
                    start.await()
                    submit(draftCase.id, 1, key).andReturn().response
                }
            }
            start.countDown()
            val results = responses.map { it.get(15, TimeUnit.SECONDS) }

            assertEquals(listOf(202, 202), results.map { it.status }.sorted())
            assertEquals(1, processingJobRepository.findByCaseRequestId(draftCase.id).size)
            assertEquals(1, caseHistoryRepository.findByCaseRequestIdOrderByOccurredAtDesc(draftCase.id).size)
            assertTrue(notificationRepository.findByRecipientSubjectOrderByCreatedAtDesc(ownerSubject).isEmpty())
            assertEquals(1, idempotencyRecordRepository.countByOperationAndIdempotencyKey("SUBMIT", key))
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `submit concorrente com mesma chave em recursos diferentes retorna conflito tipado`() {
        val otherCase = caseRequestRepository.save(
            CaseRequest(
                protocol = "CF-IDEMP-OTHER-${UUID.randomUUID()}",
                ownerSubject = ownerSubject,
                ownerEmail = USERNAME,
                title = "Outra solicitação idempotente",
                description = "Descrição do segundo caso para a corrida de idempotência."
            )
        )
        caseDocumentRepository.save(
            CaseDocument(
                caseRequest = otherCase,
                category = DocumentCategory.IDENTIFICACAO,
                fileName = "identificacao.pdf",
                fileSize = 128,
                contentType = "application/pdf",
                storageKey = "${otherCase.id}-identificacao.pdf",
                sha256 = "b".repeat(64),
                uploadState = UploadState.READY
            )
        )
        val key = "submit-cross-resource-${UUID.randomUUID()}"
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val responses = listOf(draftCase.id, otherCase.id).map { caseId ->
                executor.submit<MockHttpServletResponse> {
                    start.await()
                    submit(caseId, 1, key).andReturn().response
                }
            }
            start.countDown()
            val results = responses.map { it.get(15, TimeUnit.SECONDS) }

            assertEquals(listOf(202, 409), results.map { it.status }.sorted())
            val conflict = results.single { it.status == 409 }
            assertEquals("IDEMPOTENCY_KEY_REUSED", objectMapper.readTree(conflict.contentAsString).path("errorCode").asText())
            assertEquals(1, processingJobRepository.findByCaseRequestId(draftCase.id).size + processingJobRepository.findByCaseRequestId(otherCase.id).size)
            assertEquals(1, idempotencyRecordRepository.countByOperationAndIdempotencyKey("SUBMIT", key))
            assertEquals(
                1,
                caseHistoryRepository.findByCaseRequestIdOrderByOccurredAtDesc(draftCase.id).size +
                    caseHistoryRepository.findByCaseRequestIdOrderByOccurredAtDesc(otherCase.id).size
            )
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `mesma chave submit com contexto diferente retorna conflito sem criar outro job`() {
        val key = "submit-${UUID.randomUUID()}"
        submit(draftCase.id, 1, key).andExpect(status().isAccepted)

        submit(draftCase.id, 2, key)
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_KEY_REUSED"))

        assertEquals(1, processingJobRepository.findByCaseRequestId(draftCase.id).size)
        assertEquals(1, idempotencyRecordRepository.countByOperationAndIdempotencyKey("SUBMIT", key))
    }

    @Test
    fun `retry repetido com mesma chave e justificativa cria somente um job`() {
        val failedCase = caseRequestRepository.save(
            CaseRequest(
                protocol = "CF-RETRY-${UUID.randomUUID()}",
                ownerSubject = subject("case-owner"),
                ownerEmail = "case-owner",
                title = "Solicitação para retry",
                description = "Descrição para testar retry administrativo idempotente.",
                status = CaseStatus.FALHA_TECNICA,
                processingRun = 2
            )
        )
        val key = "retry-${UUID.randomUUID()}"
        val justification = "Armazenamento restaurado e validado pela equipe."

        retry(failedCase.id, key, justification).andExpect(status().isAccepted)
        retry(failedCase.id, key, justification).andExpect(status().isAccepted)
        retry(failedCase.id, key, "$justification com outro contexto")
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_KEY_REUSED"))

        assertEquals(1, processingJobRepository.findByCaseRequestId(failedCase.id).size)
        assertEquals(1, idempotencyRecordRepository.countByOperationAndIdempotencyKey("RETRY", key))
        assertEquals(1, caseHistoryRepository.findByCaseRequestIdOrderByOccurredAtDesc(failedCase.id).size)
        assertTrue(notificationRepository.findByRecipientSubjectOrderByCreatedAtDesc(failedCase.ownerSubject).isEmpty())
    }

    @Test
    fun `submit sem Idempotency-Key e rejeitado`() {
        mockMvc.perform(
            post("/api/v1/cases/{id}/submit", draftCase.id)
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"version":1}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_KEY_REQUIRED"))
    }

    @Test
    fun `submit sem documento pronto e rejeitado`() {
        val document = caseDocumentRepository.findByCaseRequestId(draftCase.id).single()
        document.uploadState = UploadState.PENDING
        caseDocumentRepository.saveAndFlush(document)

        submit(draftCase.id, 1, "submit-pending-${UUID.randomUUID()}")
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.errorCode").value("NO_READY_DOCUMENTS"))

        assertEquals(0, processingJobRepository.findByCaseRequestId(draftCase.id).size)
    }

    @Test
    fun `submission timestamp uses UTC independently of system timezone`() {
        val systemTimeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"))
            val before = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1)
            submit(draftCase.id, 1, "submit-utc-${UUID.randomUUID()}").andExpect(status().isAccepted)
            val submittedAt = requireNotNull(caseRequestRepository.findById(draftCase.id).orElseThrow().submittedAt)
            val after = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(1)
            assertTrue(!submittedAt.isBefore(before) && !submittedAt.isAfter(after))
        } finally {
            TimeZone.setDefault(systemTimeZone)
        }
    }

    @Test
    fun `retry exige ADMIN e caso em falha tecnica`() {
        val justification = "Justificativa válida para teste."
        val userKey = "retry-user-${UUID.randomUUID()}"
        mockMvc.perform(
            post("/api/v1/cases/{id}/retry", draftCase.id)
                .header("Authorization", "Bearer $userToken")
                .header("Idempotency-Key", userKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mapOf("justification" to justification)))
        ).andExpect(status().isForbidden)

        val adminKey = "retry-admin-${UUID.randomUUID()}"
        retry(draftCase.id, adminKey, justification)
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.errorCode").value("INVALID_STATE_FOR_RETRY"))

        assertEquals(0, processingJobRepository.findByCaseRequestId(draftCase.id).size)
        assertEquals(0, idempotencyRecordRepository.countByOperationAndIdempotencyKey("RETRY", adminKey))
    }

    private fun submit(id: UUID, version: Long, key: String) = mockMvc.perform(
        post("/api/v1/cases/{id}/submit", id)
            .header("Authorization", "Bearer $userToken")
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(mapOf("version" to version)))
    )

    private fun retry(id: UUID, key: String, justification: String) = mockMvc.perform(
        post("/api/v1/cases/{id}/retry", id)
            .header("Authorization", "Bearer ${token("demo-admin", "ADMIN")}")
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(mapOf("justification" to justification)))
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
        private const val USERNAME = "idempotency-user"
        private const val TEST_SECRET = "caseflow-test-only-secret-value-0123456789"
    }
}
