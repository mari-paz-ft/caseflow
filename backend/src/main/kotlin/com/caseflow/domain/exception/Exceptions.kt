package com.caseflow.domain.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime
import java.util.UUID

open class CaseFlowException(
    override val message: String,
    val status: HttpStatus = HttpStatus.BAD_REQUEST,
    val errorCode: String = "BUSINESS_ERROR"
) : RuntimeException(message)

class ResourceNotFoundException(message: String) : 
    CaseFlowException(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND")

class ForbiddenException(message: String) : 
    CaseFlowException(message, HttpStatus.FORBIDDEN, "FORBIDDEN_ACCESS")

class ConflictException(message: String, errorCode: String = "CONFLICT_STATE") : 
    CaseFlowException(message, HttpStatus.CONFLICT, errorCode)

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
        val error = ApiErrorResponse(
            status = ex.status.value(),
            error = ex.status.reasonPhrase,
            message = ex.message,
            errorCode = ex.errorCode
        )
        return ResponseEntity.status(ex.status).body(error)
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

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception): ResponseEntity<ApiErrorResponse> {
        val error = ApiErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = ex.message ?: "Ocorreu um erro interno inesperado",
            errorCode = "INTERNAL_SERVER_ERROR"
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }
}
