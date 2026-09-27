package com.caseflow.bff.infrastructure.client.auth

import com.caseflow.bff.application.port.AuthServiceClient
import com.caseflow.bff.application.port.AuthServiceLoginResult
import com.caseflow.bff.infrastructure.client.DownstreamCallExecutor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class RestAuthServiceClient(
    @Qualifier("authServiceRestClient") private val restClient: RestClient,
    private val downstreamCallExecutor: DownstreamCallExecutor
) : AuthServiceClient {
    override fun login(username: String, password: String): AuthServiceLoginResult =
        downstreamCallExecutor.execute("auth-service") {
            val response = restClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(AuthServiceLoginWireRequest(username, password))
                .retrieve()
                .body(AuthServiceLoginWireResponse::class.java)
                ?: error("auth-service retornou uma resposta vazia")
            AuthServiceLoginResult(
                accessToken = response.accessToken,
                tokenType = response.tokenType,
                expiresAt = response.expiresAt,
                sub = response.sub,
                username = response.username,
                roles = response.roles
            )
        }
}
