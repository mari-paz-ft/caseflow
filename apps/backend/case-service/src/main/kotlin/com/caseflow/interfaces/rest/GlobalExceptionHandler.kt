package com.caseflow.interfaces.rest

import com.caseflow.domain.exception.CaseFlowErrorKind
import com.caseflow.domain.exception.CaseFlowException
import jakarta.persistence.OptimisticLockException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime
import java.util.UUID

data class ApiErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val message: String,
    val errorCode: String,
    val traceId: String = UUID.randomUUID().toString(),
    val validationErrors: Map<String, String>? = null
)

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(CaseFlowException::class)
    fun handleCaseFlowException(ex: CaseFlowException): ResponseEntity<ApiErrorResponse> {
        val status = when (ex.kind) {
            CaseFlowErrorKind.BAD_REQUEST -> HttpStatus.BAD_REQUEST
            CaseFlowErrorKind.NOT_FOUND -> HttpStatus.NOT_FOUND
            CaseFlowErrorKind.FORBIDDEN -> HttpStatus.FORBIDDEN
            CaseFlowErrorKind.CONFLICT -> HttpStatus.CONFLICT
        }
        return ResponseEntity.status(status).body(
            ApiErrorResponse(
                status = status.value(),
                error = status.reasonPhrase,
                message = ex.message,
                errorCode = ex.errorCode
            )
        )
    }

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingHeader(ex: MissingRequestHeaderException): ResponseEntity<ApiErrorResponse> {
        val missingIdempotencyKey = ex.headerName == "Idempotency-Key"
        val error = ApiErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = if (missingIdempotencyKey) "Idempotency-Key é obrigatória" else "Um header obrigatório não foi informado",
            errorCode = if (missingIdempotencyKey) "IDEMPOTENCY_KEY_REQUIRED" else "MISSING_HEADER"
        )
        return ResponseEntity.badRequest().body(error)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> {
        val fieldErrors = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Valor inválido") }
        val error = ApiErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = "Erro de validação nos campos informados",
            errorCode = "VALIDATION_FAILED",
            validationErrors = fieldErrors
        )
        return ResponseEntity.badRequest().body(error)
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(): ResponseEntity<ApiErrorResponse> {
        val error = ApiErrorResponse(
            status = HttpStatus.CONFLICT.value(),
            error = HttpStatus.CONFLICT.reasonPhrase,
            message = "A operação conflita com um registro já persistido",
            errorCode = "PERSISTENCE_CONFLICT"
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error)
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException::class, OptimisticLockException::class)
    fun handleOptimisticLockConflict(): ResponseEntity<ApiErrorResponse> {
        val status = HttpStatus.CONFLICT
        return ResponseEntity.status(status).body(
            ApiErrorResponse(
                status = status.value(),
                error = status.reasonPhrase,
                message = "O recurso foi alterado por outra operação; atualize os dados e tente novamente",
                errorCode = "VERSION_MISMATCH"
            )
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception): ResponseEntity<ApiErrorResponse> {
        val error = ApiErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "Ocorreu um erro interno inesperado",
            errorCode = "INTERNAL_SERVER_ERROR"
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }
}
