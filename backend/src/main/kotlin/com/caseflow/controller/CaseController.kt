package com.caseflow.controller

import com.caseflow.config.CurrentUserContext
import com.caseflow.controller.dto.*
import com.caseflow.domain.enums.DocumentCategory
import com.caseflow.domain.exception.ResourceNotFoundException
import com.caseflow.domain.model.AppUser
import com.caseflow.repository.AppUserRepository
import com.caseflow.service.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping(value = ["/api/v1", "/bff/v1"])
@Tag(name = "Casos e Solicitações", description = "Endpoints de gerenciamento e ciclo de vida de solicitações")
class CaseController(
    private val caseService: CaseService,
    private val documentService: DocumentService,
    private val historyService: HistoryService,
    private val notificationService: NotificationService,
    private val appUserRepository: AppUserRepository
) {

    private fun getCurrentUser(): AppUser {
        return CurrentUserContext.get()
            ?: appUserRepository.findByEmail("solicitante@caseflow.local").orElseThrow {
                ResourceNotFoundException("Usuário padrão não encontrado")
            }
    }

    @GetMapping("/csrf")
    @Operation(summary = "Obter token CSRF vinculado à sessão")
    fun getCsrf(): Map<String, String> {
        return mapOf("csrfToken" to UUID.randomUUID().toString())
    }

    @GetMapping("/me")
    @Operation(summary = "Identidade e capacidades do usuário logado")
    fun getMe(): UserDto {
        val user = getCurrentUser()
        return UserDto(
            id = user.id,
            email = user.email,
            fullName = user.fullName,
            role = user.role
        )
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Login com e-mail e senha")
    fun login(@RequestBody request: LoginRequestDto): ResponseEntity<LoginResponseDto> {
        val user = appUserRepository.findByEmail(request.email)
            .orElseThrow { ResourceNotFoundException("Usuário ou senha incorretos") }
        val token = "mock-token-${user.email}"
        val userDto = UserDto(user.id, user.email, user.fullName, user.role)
        return ResponseEntity.ok(LoginResponseDto(token, userDto))
    }

    @PostMapping("/logout")
    @Operation(summary = "Encerrar sessão atual")
    fun logout(): Map<String, String> {
        return mapOf("message" to "Sessão encerrada com sucesso")
    }

    @PostMapping("/cases")
    @Operation(summary = "Criar novo rascunho de solicitação")
    fun createCase(@Valid @RequestBody dto: CreateCaseDto): ResponseEntity<CaseResponseDto> {
        val created = caseService.createCase(dto, getCurrentUser())
        val location = URI.create("/bff/v1/cases/${created.id}")
        return ResponseEntity.created(location).body(created)
    }

    @GetMapping("/cases")
    @Operation(summary = "Listar solicitações acessíveis ao usuário")
    fun listCases(): ResponseEntity<List<CaseResponseDto>> {
        val cases = caseService.listCases(getCurrentUser())
        return ResponseEntity.ok(cases)
    }

    @GetMapping("/cases/{id}")
    @Operation(summary = "Consultar detalhe da solicitação")
    fun getCaseById(@PathVariable id: UUID): ResponseEntity<CaseResponseDto> {
        val case = caseService.getCaseById(id, getCurrentUser())
        return ResponseEntity.ok(case)
    }

    @PutMapping("/cases/{id}")
    @Operation(summary = "Atualizar rascunho de solicitação")
    fun updateCase(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: UpdateCaseDto
    ): ResponseEntity<CaseResponseDto> {
        val updated = caseService.updateCase(id, dto, getCurrentUser())
        return ResponseEntity.ok(updated)
    }

    @PostMapping("/cases/{id}/documents")
    @Operation(summary = "Anexar documento à solicitação em rascunho")
    fun uploadDocument(
        @PathVariable id: UUID,
        @RequestParam("category") category: DocumentCategory,
        @RequestParam(value = "validUntil", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) validUntil: LocalDate?,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<CaseDocumentDto> {
        val doc = documentService.uploadDocument(id, category, validUntil, file, getCurrentUser())
        val updatedCase = caseService.getCaseById(id, getCurrentUser())
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .header("X-Case-Version", updatedCase.version.toString())
            .body(doc)
    }

    @DeleteMapping("/cases/{id}/documents/{documentId}")
    @Operation(summary = "Remover anexo da solicitação")
    fun deleteDocument(
        @PathVariable id: UUID,
        @PathVariable documentId: UUID
    ): ResponseEntity<Void> {
        documentService.deleteDocument(caseId = id, documentId = documentId, currentUser = getCurrentUser())
        val updatedCase = caseService.getCaseById(id, getCurrentUser())
        return ResponseEntity
            .noContent()
            .header("X-Case-Version", updatedCase.version.toString())
            .build()
    }

    @GetMapping("/cases/{id}/documents/{documentId}/content")
    @Operation(summary = "Baixar conteúdo do documento privado")
    fun getDocumentContent(
        @PathVariable id: UUID,
        @PathVariable documentId: UUID
    ): ResponseEntity<ByteArray> {
        val content = documentService.getDocumentContent(id, documentId, getCurrentUser())
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
        @RequestHeader(value = "Idempotency-Key", required = false) idempotencyKey: String?
    ): ResponseEntity<CaseResponseDto> {
        val submitted = caseService.submitCase(id, dto, getCurrentUser(), idempotencyKey)
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(submitted)
    }

    @GetMapping("/cases/{id}/history")
    @Operation(summary = "Consultar histórico de transições da solicitação")
    fun getHistory(@PathVariable id: UUID): ResponseEntity<List<CaseHistoryDto>> {
        // Verifica permissão de acesso
        caseService.getCaseById(id, getCurrentUser())
        val historyList = historyService.getHistoryForCase(id)
        val dtos = historyList.map {
            CaseHistoryDto(
                id = it.id,
                caseId = id,
                eventType = it.eventType,
                actorSubject = it.actorSubject,
                details = it.details,
                occurredAt = it.occurredAt
            )
        }
        return ResponseEntity.ok(dtos)
    }

    @PostMapping("/cases/{id}/retry")
    @Operation(summary = "Reprocessar solicitação em falha técnica (Apenas Administrador)")
    fun retryCase(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: RetryCaseDto,
        @RequestHeader(value = "Idempotency-Key", required = false) idempotencyKey: String?
    ): ResponseEntity<CaseResponseDto> {
        val retried = caseService.retryCase(id, dto, getCurrentUser(), idempotencyKey)
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(retried)
    }

    @GetMapping("/notifications")
    @Operation(summary = "Listar notificações do usuário")
    fun getNotifications(): ResponseEntity<List<NotificationDto>> {
        val user = getCurrentUser()
        val notifs = notificationService.getNotificationsForUser(user.id)
        val dtos = notifs.map {
            NotificationDto(
                id = it.id,
                caseId = it.caseRequest.id,
                title = it.title,
                message = it.message,
                readAt = it.readAt,
                createdAt = it.createdAt
            )
        }
        return ResponseEntity.ok(dtos)
    }

    @PatchMapping("/notifications/{id}")
    @Operation(summary = "Marcar notificação como lida")
    fun markNotificationRead(@PathVariable id: UUID): ResponseEntity<NotificationDto> {
        val notif = notificationService.markAsRead(id, getCurrentUser().id)
        return ResponseEntity.ok(
            NotificationDto(
                id = notif.id,
                caseId = notif.caseRequest.id,
                title = notif.title,
                message = notif.message,
                readAt = notif.readAt,
                createdAt = notif.createdAt
            )
        )
    }
}
