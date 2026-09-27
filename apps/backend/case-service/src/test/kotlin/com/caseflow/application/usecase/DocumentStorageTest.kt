package com.caseflow.application.usecase

import com.caseflow.application.port.CaseDocumentRepository
import com.caseflow.application.port.CaseHistoryRepository
import com.caseflow.application.port.CaseRequestRepository
import com.caseflow.domain.enums.DocumentCategory
import com.caseflow.domain.enums.RoleName
import com.caseflow.domain.enums.UploadState
import com.caseflow.domain.exception.CaseFlowException
import com.caseflow.domain.exception.ForbiddenException
import com.caseflow.domain.exception.TechnicalFailureException
import com.caseflow.domain.model.CaseActor
import com.caseflow.domain.model.CaseDocument
import com.caseflow.domain.model.CaseHistory
import com.caseflow.application.dto.UploadDocumentCommand
import com.caseflow.domain.model.CaseRequest
import com.caseflow.domain.rule.PdfDocumentRules
import com.caseflow.infrastructure.storage.LocalFileSystemDocumentStorage
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito
import java.nio.file.Path
import java.util.Optional
import java.util.UUID

class DocumentStorageTest {
    @TempDir
    private lateinit var tempDirectory: Path

    private lateinit var caseRequestRepository: CaseRequestRepository
    private lateinit var caseDocumentRepository: CaseDocumentRepository
    private lateinit var historyRepository: CaseHistoryRepository
    private lateinit var documentStorage: LocalFileSystemDocumentStorage
    private lateinit var documentService: DocumentService
    private lateinit var caseRequest: CaseRequest
    private lateinit var actor: CaseActor

    private val validPdf = "%PDF-1.4\nCaseFlow test document\n%%EOF".toByteArray()

    @BeforeEach
    fun setUp() {
        caseDocumentRepository = Mockito.mock(
            CaseDocumentRepository::class.java,
            Mockito.withSettings().defaultAnswer { invocation ->
                if (invocation.method.name == "save" || invocation.method.name == "saveAndFlush") invocation.arguments[0] else null
            }
        )
        historyRepository = Mockito.mock(
            CaseHistoryRepository::class.java,
            Mockito.withSettings().defaultAnswer { invocation ->
                if (invocation.method.name == "save") invocation.arguments[0] else null
            }
        )
        documentStorage = LocalFileSystemDocumentStorage(tempDirectory.resolve("documents").toString())
        actor = CaseActor(UUID.randomUUID(), "document-user", setOf(RoleName.ROLE_USER))
        caseRequest = CaseRequest(
            protocol = "CF-DOC-${UUID.randomUUID()}",
            ownerSubject = actor.subject,
            ownerEmail = actor.username,
            title = "Solicitação documental",
            description = "Descrição longa o suficiente para o teste de storage documental."
        )
        caseRequestRepository = Mockito.mock(CaseRequestRepository::class.java)
        Mockito.`when`(caseRequestRepository.findById(caseRequest.id)).thenReturn(Optional.of(caseRequest))
        documentService = DocumentService(
            caseRequestRepository,
            caseDocumentRepository,
            HistoryService(historyRepository),
            documentStorage
        )
    }

    @Test
    fun `PDF com MIME e assinatura aceitos e persistido no storage local`() {
        val result = documentService.uploadDocument(
            caseRequest.id,
            uploadCommand(validPdf, "application/pdf"),
            actor
        )

        val storedDocument = caseRequest.documents.single()
        assertEquals("application/pdf", result.contentType)
        assertEquals(validPdf.size.toLong(), result.fileSize)
        assertArrayEquals(validPdf, documentStorage.read(storedDocument.storageKey))
        assertEquals(PdfDocumentRules.sha256(validPdf), storedDocument.sha256)
    }

    @Test
    fun `substitui anexo da mesma categoria sem ultrapassar tres documentos ativos`() {
        DocumentCategory.entries.forEach { category ->
            documentService.uploadDocument(caseRequest.id, uploadCommand(validPdf, "application/pdf", category), actor)
        }
        val existing = caseRequest.documents.single { it.category == DocumentCategory.IDENTIFICACAO }
        val oldStorageKey = existing.storageKey

        val replacement = documentService.uploadDocument(caseRequest.id, uploadCommand(validPdf, "application/pdf"), actor)

        assertNotEquals(existing.id, replacement.id)
        assertEquals(3, caseRequest.documents.size)
        assertEquals(3, caseRequest.documents.map { it.category }.distinct().size)
        assertArrayEquals(validPdf, documentStorage.read(caseRequest.documents.single { it.category == DocumentCategory.IDENTIFICACAO }.storageKey))
        assertThrows(TechnicalFailureException::class.java) { documentStorage.read(oldStorageKey) }
    }

    @Test
    fun `aceita as tres categorias permitidas sem criar documentos ativos duplicados`() {
        DocumentCategory.entries.forEach { category ->
            documentService.uploadDocument(caseRequest.id, uploadCommand(validPdf, "application/pdf", category), actor)
        }

        assertEquals(3, caseRequest.documents.size)
        assertEquals(3, caseRequest.documents.map { it.category }.distinct().size)
    }

    @Test
    fun `arquivo maior que cinco MiB e rejeitado`() {
        val bytes = ByteArray((PdfDocumentRules.MAX_SIZE_BYTES + 1).toInt())
        "%PDF-".toByteArray().copyInto(bytes)

        assertThrows(CaseFlowException::class.java) {
            documentService.uploadDocument(
                caseRequest.id,
                uploadCommand(bytes, "application/pdf"),
                actor
            )
        }
        assertEquals(0, caseRequest.documents.size)
    }

    @Test
    fun `MIME diferente de application pdf e rejeitado`() {
        assertThrows(CaseFlowException::class.java) {
            documentService.uploadDocument(
                caseRequest.id,
                uploadCommand(validPdf, "text/plain"),
                actor
            )
        }
        assertEquals(0, caseRequest.documents.size)
    }

    @Test
    fun `assinatura PDF ausente e rejeitada`() {
        assertThrows(CaseFlowException::class.java) {
            documentService.uploadDocument(
                caseRequest.id,
                uploadCommand("conteúdo não PDF".toByteArray(), "application/pdf"),
                actor
            )
        }
        assertEquals(0, caseRequest.documents.size)
    }

    @Test
    fun `arquivo removido do storage gera falha tecnica no download`() {
        val storageKey = "removed-document.pdf"
        documentStorage.store(storageKey, validPdf)
        val document = CaseDocument(
            caseRequest = caseRequest,
            category = DocumentCategory.IDENTIFICACAO,
            fileName = "identificacao.pdf",
            fileSize = validPdf.size.toLong(),
            contentType = "application/pdf",
            storageKey = storageKey,
            sha256 = PdfDocumentRules.sha256(validPdf),
            uploadState = UploadState.READY
        )
        caseRequest.documents.add(document)
        documentStorage.delete(storageKey)

        assertThrows(TechnicalFailureException::class.java) {
            documentService.getDocumentContent(caseRequest.id, document.id, actor)
        }
    }

    @Test
    fun `download rejects stored file whose size differs from metadata`() {
        val document = storeDocument(fileSize = validPdf.size.toLong() + 1)

        assertThrows(TechnicalFailureException::class.java) {
            documentService.getDocumentContent(caseRequest.id, document.id, actor)
        }
    }

    @Test
    fun `download rejects stored file whose hash differs from metadata`() {
        val document = storeDocument(sha256 = "0".repeat(64))

        assertThrows(TechnicalFailureException::class.java) {
            documentService.getDocumentContent(caseRequest.id, document.id, actor)
        }
    }

    @Test
    fun `download document rejects another owner`() {
        val storageKey = "private-document.pdf"
        documentStorage.store(storageKey, validPdf)
        val document = CaseDocument(
            caseRequest = caseRequest,
            category = DocumentCategory.IDENTIFICACAO,
            fileName = "identificacao.pdf",
            fileSize = validPdf.size.toLong(),
            contentType = "application/pdf",
            storageKey = storageKey,
            sha256 = PdfDocumentRules.sha256(validPdf),
            uploadState = UploadState.READY
        )
        caseRequest.documents.add(document)
        val anotherUser = CaseActor(UUID.randomUUID(), "another-user", setOf(RoleName.ROLE_USER))

        assertThrows(ForbiddenException::class.java) {
            documentService.getDocumentContent(caseRequest.id, document.id, anotherUser)
        }
    }

    private fun storeDocument(
        fileSize: Long = validPdf.size.toLong(),
        sha256: String = PdfDocumentRules.sha256(validPdf)
    ): CaseDocument {
        val storageKey = "integrity-${UUID.randomUUID()}.pdf"
        documentStorage.store(storageKey, validPdf)
        return CaseDocument(
            caseRequest = caseRequest,
            category = DocumentCategory.IDENTIFICACAO,
            fileName = "identificacao.pdf",
            fileSize = fileSize,
            contentType = "application/pdf",
            storageKey = storageKey,
            sha256 = sha256,
            uploadState = UploadState.READY
        ).also(caseRequest.documents::add)
    }

    private fun uploadCommand(content: ByteArray, contentType: String, category: DocumentCategory = DocumentCategory.IDENTIFICACAO) = UploadDocumentCommand(
        category = category,
        validUntil = null,
        contentType = contentType,
        originalFileName = "document.pdf",
        content = content
    )
}
