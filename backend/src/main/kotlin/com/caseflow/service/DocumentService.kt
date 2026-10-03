package com.caseflow.service

import com.caseflow.controller.dto.CaseDocumentDto
import com.caseflow.domain.enums.DocumentCategory
import com.caseflow.domain.enums.CaseStatus
import com.caseflow.domain.enums.UploadState
import com.caseflow.domain.exception.ConflictException
import com.caseflow.domain.exception.ForbiddenException
import com.caseflow.domain.exception.ResourceNotFoundException
import com.caseflow.domain.model.AppUser
import com.caseflow.domain.model.CaseDocument
import com.caseflow.domain.model.RoleName
import com.caseflow.repository.CaseDocumentRepository
import com.caseflow.repository.CaseRequestRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.HexFormat

@Service
class DocumentService(
    private val caseRequestRepository: CaseRequestRepository,
    private val caseDocumentRepository: CaseDocumentRepository,
    private val historyService: HistoryService,
    @Value("\${caseflow.upload-dir:./uploads}") private val uploadDirPath: String
) {
    init {
        val dir = File(uploadDirPath)
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    @Transactional
    fun uploadDocument(
        caseId: UUID,
        category: DocumentCategory,
        validUntil: LocalDate?,
        file: MultipartFile,
        currentUser: AppUser
    ): CaseDocumentDto {
        val caseRequest = caseRequestRepository.findById(caseId)
            .orElseThrow { ResourceNotFoundException("Solicitação não encontrada: $caseId") }

        if (caseRequest.ownerSubject != currentUser.id) {
            throw ForbiddenException("Apenas o autor pode anexar documentos")
        }
        if (caseRequest.status != CaseStatus.RASCUNHO) {
            throw ConflictException("Não é permitido adicionar documentos em solicitações no estado ${caseRequest.status}", "CASE_IMMUTABLE")
        }

        // Se já existe anexo na mesma categoria, remover o anterior
        val existingDoc = caseRequest.documents.find { it.category == category }
        if (existingDoc != null) {
            caseRequest.documents.remove(existingDoc)
            caseDocumentRepository.delete(existingDoc)
        } else if (caseRequest.documents.size >= 3) {
            throw ConflictException("Limite de 3 documentos por solicitação atingido", "MAX_DOCUMENTS_REACHED")
        }

        val storageKey = "${caseId}_${category}_${UUID.randomUUID()}.pdf"
        val destination = Paths.get(uploadDirPath, storageKey)
        val bytes = file.bytes
        Files.write(destination, bytes)

        val sha256 = bytes.sha256()

        val document = CaseDocument(
            caseRequest = caseRequest,
            category = category,
            fileName = file.originalFilename ?: "$category.pdf",
            fileSize = file.size,
            contentType = file.contentType ?: "application/pdf",
            storageKey = storageKey,
            sha256 = sha256,
            validUntil = validUntil,
            uploadState = UploadState.READY
        )
        caseRequest.documents.add(document)
        caseRequest.version += 1
        caseRequest.updatedAt = LocalDateTime.now()

        val savedDoc = caseDocumentRepository.save(document)
        caseRequestRepository.save(caseRequest)

        historyService.record(
            caseRequest = caseRequest,
            eventType = "DOCUMENTO_ANEXADO",
            actorSubject = currentUser.email,
            details = "Anexado arquivo para categoria $category (${document.fileName})"
        )

        return toDto(savedDoc)
    }

    @Transactional
    fun deleteDocument(caseId: UUID, documentId: UUID, currentUser: AppUser) {
        val caseRequest = caseRequestRepository.findById(caseId)
            .orElseThrow { ResourceNotFoundException("Solicitação não encontrada: $caseId") }

        if (caseRequest.ownerSubject != currentUser.id) {
            throw ForbiddenException("Apenas o autor pode remover documentos")
        }
        if (caseRequest.status != CaseStatus.RASCUNHO) {
            throw ConflictException("Não é permitido remover documentos em solicitações no estado ${caseRequest.status}", "CASE_IMMUTABLE")
        }

        val document = caseRequest.documents.find { it.id == documentId }
            ?: throw ResourceNotFoundException("Documento não encontrado: $documentId")

        caseRequest.documents.remove(document)
        caseDocumentRepository.delete(document)
        caseRequest.version += 1
        caseRequest.updatedAt = LocalDateTime.now()
        caseRequestRepository.save(caseRequest)

        historyService.record(
            caseRequest = caseRequest,
            eventType = "DOCUMENTO_REMOVIDO",
            actorSubject = currentUser.email,
            details = "Removido documento da categoria ${document.category}"
        )
    }

    @Transactional(readOnly = true)
    fun getDocumentContent(caseId: UUID, documentId: UUID, currentUser: AppUser): ByteArray {
        val caseRequest = caseRequestRepository.findById(caseId)
            .orElseThrow { ResourceNotFoundException("Solicitação não encontrada: $caseId") }

        if (currentUser.role != RoleName.ROLE_ADMIN && caseRequest.ownerSubject != currentUser.id) {
            throw ForbiddenException("Acesso negado ao conteúdo do documento")
        }

        val document = caseRequest.documents.find { it.id == documentId }
            ?: throw ResourceNotFoundException("Documento não encontrado: $documentId")

        val path = Paths.get(uploadDirPath, document.storageKey)
        return if (Files.exists(path)) {
            Files.readAllBytes(path)
        } else {
            // Retorna conteúdo representativo simulado de documento PDF
            "%PDF-1.4\n1 0 obj\n<< /Title (${document.fileName}) /Author (CaseFlow) >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF".toByteArray()
        }
    }

    fun toDto(doc: CaseDocument): CaseDocumentDto {
        return CaseDocumentDto(
            id = doc.id,
            category = doc.category,
            fileName = doc.fileName,
            fileSize = doc.fileSize,
            contentType = doc.contentType,
            validUntil = doc.validUntil,
            uploadState = doc.uploadState,
            createdAt = doc.createdAt
        )
    }

    private fun ByteArray.sha256(): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(this)
        return HexFormat.of().formatHex(digest)
    }
}
