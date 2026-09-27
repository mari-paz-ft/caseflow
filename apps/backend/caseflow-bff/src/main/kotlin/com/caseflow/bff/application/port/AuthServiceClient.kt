package com.caseflow.bff.application.port

import java.time.Instant

interface AuthServiceClient {
    fun login(username: String, password: String): AuthServiceLoginResult
}

data class AuthServiceLoginResult(
    val accessToken: String,
    val tokenType: String,
    val expiresAt: Instant,
    val sub: String,
    val username: String,
    val roles: Set<String>
)
