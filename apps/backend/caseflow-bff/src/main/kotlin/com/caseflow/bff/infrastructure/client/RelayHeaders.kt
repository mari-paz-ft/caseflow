package com.caseflow.bff.infrastructure.client

import com.caseflow.bff.application.port.DownstreamRequestHeaders
import org.springframework.http.HttpHeaders

internal fun HttpHeaders.copyRelayHeaders(source: DownstreamRequestHeaders) {
    set(HttpHeaders.AUTHORIZATION, source.authorization)
    source.idempotencyKey?.let { set("Idempotency-Key", it) }
    source.correlationId?.let { set("Correlation-Id", it) }
}
