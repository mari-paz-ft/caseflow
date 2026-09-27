package com.caseflow.bff.infrastructure.client.auth

import java.time.Instant

data class AuthServiceLoginWireRequest(val username: String, val password: String)

data class AuthServiceLoginWireResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresAt: Instant,
    val sub: String,
    val username: String,
    val roles: Set<String>
)
