package com.caseflow.application.usecase

import com.caseflow.domain.exception.ConflictException
import com.caseflow.interfaces.rest.GlobalExceptionHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class GlobalExceptionHandlerTest {
    @Test
    fun `domain conflict maps to HTTP conflict without framework dependency in domain`() {
        val response = GlobalExceptionHandler().handleCaseFlowException(
            ConflictException("Versão divergente", "VERSION_MISMATCH")
        )

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals("VERSION_MISMATCH", response.body?.errorCode)
    }

    @Test
    fun `unexpected exception details are not returned to the client`() {
        val response = GlobalExceptionHandler().handleGeneralException(
            IllegalStateException("internal database host and query details")
        )

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("Ocorreu um erro interno inesperado", response.body?.message)
    }
}
