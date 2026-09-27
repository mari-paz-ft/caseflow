package com.caseflow.application.usecase

import com.caseflow.application.dto.CaseDocumentDto
import com.caseflow.application.dto.UploadDocumentCommand
import com.caseflow.application.port.DocumentStoragePort
import com.caseflow.domain.enums.DocumentCategory
import com.caseflow.domain.enums.CaseStatus
import com.caseflow.domain.enums.UploadState
import com.caseflow.domain.exception.CaseFlowException
import com.caseflow.domain.exception.ConflictException
import com.caseflow.domain.exception.TechnicalFailureException
import com.caseflow.domain.exception.ResourceNotFoundException
import com.caseflow.domain.model.CaseActor
import com.caseflow.domain.model.CaseDocument
import com.caseflow.application.port.CaseDocumentRepository
import com.caseflow.application.port.CaseRequestRepository
import com.caseflow.domain.rule.CaseAuthorizationPolicy
import com.caseflow.domain.rule.PdfDocumentRules
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class DocumentService(
    private val caseRequestRepository: CaseRequestRepository,
    private val caseDocumentRepository: CaseDocumentRepository,
    private val historyService: HistoryService,
    private val documentStoragePort: DocumentStoragePort
) {

    @Transactional
    fun uploadDocument(
        caseId: UUID,
        command: UploadDocumentCommand,
        currentUser: CaseActor
    ): CaseDocumentDto {
        val caseRequest = caseRequestRepository.findById(caseId)
            .orElseThrow { ResourceNotFoundException("Solicitação não encontrada: $caseId") }

        CaseAuthorizationPolicy.requireOwner(caseRequest, currentUser)
        if (caseRequest.status != CaseStatus.RASCUNHO) {
            throw ConflictException("Não é permitido adicionar documentos em solicitações no estado ${caseRequest.status}", "CASE_IMMUTABLE")
        }

        val bytes = command.content
        if (!PdfDocumentRules.hasPdfMime(command.contentType)) {
            throw CaseFlowException("O arquivo deve usar MIME application/pdf", errorCode = "INVALID_DOCUMENT_MIME")
        }
        if (bytes.size.toLong() > PdfDocumentRules.MAX_SIZE_BYTES) {
            throw CaseFlowException("O PDF excede o limite de 5 MiB", errorCode = "DOCUMENT_TOO_LARGE")
        }
        if (!PdfDocumentRules.hasPdfSignature(bytes)) {
            throw CaseFlowException("O arquivo não possui assinatura PDF válida", errorCode = "INVALID_PDF_SIGNATURE")
        }

        // Se já existe anexo na mesma categoria, remover o anterior
        val existingDoc = caseRequest.documents.find { it.category == command.category }
        if (existingDoc == null && caseRequest.documents.size >= 3) {
            throw ConflictException("Limite de 3 documentos por solicitação atingido", "MAX_DOCUMENTS_REACHED")
        }

        val storageKey = "${caseId}_${command.category}_${UUID.randomUUID()}.pdf"
        documentStoragePort.store(storageKey, bytes)
        registerStorageCleanup(storageKeyOnRollback = storageKey, storageKeyOnCommit = existingDoc?.storageKey)

        if (existingDoc != null) {
            caseRequest.documents.remove(existingDoc)
            caseDocumentRepository.delete(existingDoc)
        }

        val document = CaseDocument(
            caseRequest = caseRequest,
            category = command.category,
            fileName = command.originalFileName ?: "${command.category}.pdf",
            fileSize = bytes.size.toLong(),
            contentType = "application/pdf",
            storageKey = storageKey,
            sha256 = PdfDocumentRules.sha256(bytes),
            validUntil = command.validUntil,
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
            actorSubject = currentUser.subject.toString(),
            details = "Anexado arquivo para categoria ${command.category} (${document.fileName})"
        )

        return toDto(savedDoc)
    }

    @Transactional
    fun deleteDocument(caseId: UUID, documentId: UUID, currentUser: CaseActor) {
        val caseRequest = caseRequestRepository.findById(caseId)
            .orElseThrow { ResourceNotFoundException("Solicitação não encontrada: $caseId") }

        CaseAuthorizationPolicy.requireOwner(caseRequest, currentUser)
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
        registerStorageCleanup(storageKeyOnCommit = document.storageKey)

        historyService.record(
            caseRequest = caseRequest,
            eventType = "DOCUMENTO_REMOVIDO",
            actorSubject = currentUser.subject.toString(),
            details = "Removido documento da categoria ${document.category}"
        )
    }

    @Transactional(readOnly = true)
    fun getDocumentContent(caseId: UUID, documentId: UUID, currentUser: CaseActor): ByteArray {
        val caseRequest = caseRequestRepository.findById(caseId)
            .orElseThrow { ResourceNotFoundException("Solicitação não encontrada: $caseId") }

        CaseAuthorizationPolicy.requireReadAccess(caseRequest, currentUser)

        val document = caseRequest.documents.find { it.id == documentId }
            ?: throw ResourceNotFoundException("Documento não encontrado: $documentId")

        val content = documentStoragePort.read(document.storageKey)
        if (!PdfDocumentRules.isAcceptableUpload(document.contentType, content) ||
            content.size.toLong() != document.fileSize ||
            PdfDocumentRules.sha256(content) != document.sha256
        ) {
            throw TechnicalFailureException("Documento armazenado está corrompido ou indisponível")
        }
        return content
    }

    private fun registerStorageCleanup(storageKeyOnRollback: String? = null, storageKeyOnCommit: String? = null) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            storageKeyOnCommit?.let(documentStoragePort::delete)
            return
        }
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCompletion(status: Int) {
                val storageKey = when (status) {
                    TransactionSynchronization.STATUS_COMMITTED -> storageKeyOnCommit
                    TransactionSynchronization.STATUS_ROLLED_BACK -> storageKeyOnRollback
                    else -> null
                }
                storageKey?.let { runCatching { documentStoragePort.delete(it) } }
            }
        })
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

}
