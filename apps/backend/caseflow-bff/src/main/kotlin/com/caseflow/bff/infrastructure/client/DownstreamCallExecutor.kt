package com.caseflow.bff.infrastructure.client

import com.caseflow.bff.application.exception.DownstreamServiceException
import com.caseflow.bff.infrastructure.client.case.CaseServiceErrorWire
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientResponseException

@Component
class DownstreamCallExecutor(
    private val objectMapper: ObjectMapper
) {
    fun <T> execute(service: String, call: () -> T): T = try {
        call()
    } catch (exception: RestClientResponseException) {
        val error = runCatching {
            objectMapper.readValue(exception.responseBodyAsByteArray, CaseServiceErrorWire::class.java)
        }.getOrNull()
        val status = exception.statusCode.value()
        val message = if (status >= 500) "$service está indisponível" else error?.message ?: "A solicitação foi rejeitada pelo serviço"
        throw DownstreamServiceException(
            statusCode = status,
            errorCode = error?.errorCode ?: "${service.uppercase()}_ERROR",
            message = message,
            traceId = error?.traceId
        )
    } catch (exception: ResourceAccessException) {
        throw DownstreamServiceException(
            statusCode = 503,
            errorCode = "${service.uppercase()}_UNAVAILABLE",
            message = "$service está indisponível",
            traceId = null
        )
    }
}
