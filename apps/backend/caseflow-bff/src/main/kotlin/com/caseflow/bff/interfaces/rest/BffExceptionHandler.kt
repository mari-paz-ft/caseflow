package com.caseflow.bff.interfaces.rest

import com.caseflow.bff.application.dto.BffErrorResponse
import com.caseflow.bff.application.exception.DownstreamServiceException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.UUID

@RestControllerAdvice
class BffExceptionHandler {
    @ExceptionHandler(DownstreamServiceException::class)
    fun handleDownstream(exception: DownstreamServiceException): ResponseEntity<BffErrorResponse> {
        val body = BffErrorResponse(
            status = exception.statusCode,
            errorCode = exception.errorCode,
            message = exception.message,
            traceId = exception.traceId ?: UUID.randomUUID().toString()
        )
        return ResponseEntity.status(HttpStatusCode.valueOf(exception.statusCode)).body(body)
    }

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingHeader(
        exception: MissingRequestHeaderException,
        request: HttpServletRequest
    ): ResponseEntity<BffErrorResponse> {
        val missingIdempotencyKey = exception.headerName == "Idempotency-Key"
        return ResponseEntity.badRequest().body(
            BffErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                errorCode = if (missingIdempotencyKey) "IDEMPOTENCY_KEY_REQUIRED" else "MISSING_HEADER",
                message = if (missingIdempotencyKey) "Idempotency-Key é obrigatória" else "Um header obrigatório não foi informado",
                traceId = request.getHeader("Correlation-Id") ?: UUID.randomUUID().toString()
            )
        )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(request: HttpServletRequest): ResponseEntity<BffErrorResponse> =
        ResponseEntity.badRequest().body(
            BffErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                errorCode = "VALIDATION_FAILED",
                message = "Os dados enviados são inválidos",
                traceId = request.getHeader("Correlation-Id") ?: UUID.randomUUID().toString()
            )
        )

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(request: HttpServletRequest): ResponseEntity<BffErrorResponse> =
        ResponseEntity.internalServerError().body(
            BffErrorResponse(
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                errorCode = "INTERNAL_SERVER_ERROR",
                message = "Ocorreu um erro interno no BFF",
                traceId = request.getHeader("Correlation-Id") ?: UUID.randomUUID().toString()
            )
        )
}
