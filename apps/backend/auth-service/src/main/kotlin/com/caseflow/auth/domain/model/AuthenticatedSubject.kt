package com.caseflow.auth.domain.model

enum class AuthRole {
    USER,
    ADMIN
}

data class AuthenticatedSubject(
    val sub: String,
    val username: String,
    val roles: Set<AuthRole>
)
