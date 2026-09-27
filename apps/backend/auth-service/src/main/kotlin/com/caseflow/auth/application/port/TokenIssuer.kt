package com.caseflow.auth.application.port

import com.caseflow.auth.domain.model.AuthRole
import java.time.Instant

interface TokenIssuer {
    fun issue(subject: String, username: String, roles: Set<AuthRole>): IssuedToken
}

data class IssuedToken(
    val value: String,
    val expiresAt: Instant
)
