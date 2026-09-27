package com.caseflow.interfaces.rest

import com.caseflow.application.dto.CaseDocumentDto
import com.caseflow.application.dto.CaseHistoryDto
import com.caseflow.application.dto.CaseResponseDto
import com.caseflow.application.dto.CreateCaseDto
import com.caseflow.application.dto.NotificationDto
import com.caseflow.application.dto.RetryCaseDto
import com.caseflow.application.dto.SubmitCaseDto
import com.caseflow.application.dto.UpdateCaseDto
import com.caseflow.application.dto.UploadDocumentCommand
import com.caseflow.application.port.CurrentCaseActorProvider
import com.caseflow.application.usecase.CaseService
import com.caseflow.application.usecase.DocumentService
import com.caseflow.application.usecase.HistoryService
import com.caseflow.application.usecase.NotificationService
import com.caseflow.domain.enums.DocumentCategory
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping(CaseApiPaths.VERSIONED)
@Tag(name = "Casos e Solicitações", description = "Endpoints internos de gerenciamento e ciclo de vida de solicitações")
class CaseController(
    private val caseService: CaseService,
    private val documentService: DocumentService,
    private val historyService: HistoryService,
    private val notificationService: NotificationService,
    private val currentCaseActorProvider: CurrentCaseActorProvider
) {
    private fun currentActor() = currentCaseActorProvider.currentActor()

    @PostMapping("/cases")
    @Operation(summary = "Criar novo rascunho de solicitação")
    fun createCase(@Valid @RequestBody dto: CreateCaseDto): ResponseEntity<CaseResponseDto> {
        val created = caseService.createCase(dto, currentActor())
        val location = URI.create("${CaseApiPaths.VERSIONED}/cases/${created.id}")
        return ResponseEntity.created(location).body(created)
    }

    @GetMapping("/cases")
    @Operation(summary = "Listar solicitações acessíveis ao usuário")
    fun listCases(): ResponseEntity<List<CaseResponseDto>> = ResponseEntity.ok(caseService.listCases(currentActor()))

    @GetMapping("/cases/{id}")
    @Operation(summary = "Consultar detalhe da solicitação")
    fun getCaseById(@PathVariable id: UUID): ResponseEntity<CaseResponseDto> =
        ResponseEntity.ok(caseService.getCaseById(id, currentActor()))

    @PutMapping("/cases/{id}")
    @Operation(summary = "Atualizar rascunho de solicitação")
    fun updateCase(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateCaseDto
    ): ResponseEntity<CaseResponseDto> = ResponseEntity.ok(caseService.updateCase(id, dto, currentActor()))

    @PostMapping("/cases/{id}/documents")
    @Operation(summary = "Anexar documento à solicitação em rascunho")
    fun uploadDocument(
        @PathVariable id: UUID,
        @RequestParam("category") category: DocumentCategory,
        @RequestParam(value = "validUntil", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) validUntil: LocalDate?,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<CaseDocumentDto> {
        val actor = currentActor()
        val command = UploadDocumentCommand(
            category = category,
            validUntil = validUntil,
            contentType = file.contentType,
            originalFileName = file.originalFilename,
            content = file.bytes
        )
        val document = documentService.uploadDocument(id, command, actor)
        val updatedCase = caseService.getCaseById(id, actor)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .header("X-Case-Version", updatedCase.version.toString())
            .body(document)
    }

    @DeleteMapping("/cases/{id}/documents/{documentId}")
    @Operation(summary = "Remover anexo da solicitação")
    fun deleteDocument(@PathVariable id: UUID, @PathVariable documentId: UUID): ResponseEntity<Void> {
        val actor = currentActor()
        documentService.deleteDocument(caseId = id, documentId = documentId, currentUser = actor)
        val updatedCase = caseService.getCaseById(id, actor)
        return ResponseEntity.noContent()
            .header("X-Case-Version", updatedCase.version.toString())
            .build()
    }

    @GetMapping("/cases/{id}/documents/{documentId}/content")
    @Operation(summary = "Baixar conteúdo do documento privado")
    fun getDocumentContent(@PathVariable id: UUID, @PathVariable documentId: UUID): ResponseEntity<ByteArray> {
        val content = documentService.getDocumentContent(id, documentId, currentActor())
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"document-$documentId.pdf\"")
            .body(content)
    }

    @PostMapping("/cases/{id}/submit")
    @Operation(summary = "Enviar solicitação para conferência automática")
    fun submitCase(
        @PathVariable id: UUID,
        @RequestBody dto: SubmitCaseDto,
        @RequestHeader(value = "Idempotency-Key") idempotencyKey: String
    ): ResponseEntity<CaseResponseDto> {
        val result = caseService.submitCase(id, dto, currentActor(), idempotencyKey)
        return ResponseEntity.status(result.responseStatus).body(result.responseBody)
    }

    @GetMapping("/cases/{id}/history")
    @Operation(summary = "Consultar histórico de transições da solicitação")
    fun getHistory(@PathVariable id: UUID): ResponseEntity<List<CaseHistoryDto>> {
        // Verifica permissão de acesso
        caseService.getCaseById(id, currentActor())
        val history = historyService.getHistoryForCase(id).map {
            CaseHistoryDto(
                id = it.id,
                caseId = id,
                eventType = it.eventType,
                actorSubject = it.actorSubject,
                details = it.details,
                occurredAt = it.occurredAt
            )
        }
        return ResponseEntity.ok(history)
    }

    @PostMapping("/cases/{id}/retry")
    @Operation(summary = "Reprocessar solicitação em falha técnica (Apenas Administrador)")
    fun retryCase(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: RetryCaseDto,
        @RequestHeader(value = "Idempotency-Key") idempotencyKey: String
    ): ResponseEntity<CaseResponseDto> {
        val result = caseService.retryCase(id, dto, currentActor(), idempotencyKey)
        return ResponseEntity.status(result.responseStatus).body(result.responseBody)
    }

    @GetMapping("/notifications")
    @Operation(summary = "Listar notificações do usuário")
    fun getNotifications(): ResponseEntity<List<NotificationDto>> {
        val notifications = notificationService.getNotificationsForUser(currentActor().subject).map {
            NotificationDto(
                id = it.id,
                caseId = it.caseRequest.id,
                title = it.title,
                message = it.message,
                readAt = it.readAt,
                createdAt = it.createdAt
            )
        }
        return ResponseEntity.ok(notifications)
    }

    @PatchMapping("/notifications/{id}")
    @Operation(summary = "Marcar notificação como lida")
    fun markNotificationRead(@PathVariable id: UUID): ResponseEntity<NotificationDto> {
        val notification = notificationService.markAsRead(id, currentActor().subject)
        return ResponseEntity.ok(
            NotificationDto(
                id = notification.id,
                caseId = notification.caseRequest.id,
                title = notification.title,
                message = notification.message,
                readAt = notification.readAt,
                createdAt = notification.createdAt
            )
        )
    }
}
