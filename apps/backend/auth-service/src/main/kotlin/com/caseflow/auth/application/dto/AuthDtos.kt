package com.caseflow.auth.application.dto

import com.caseflow.auth.domain.model.AuthRole
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class LoginRequest(
    @field:NotBlank val username: String,
    @field:NotBlank val password: String
)

data class LoginResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresAt: Instant,
    val sub: String,
    val username: String,
    val roles: Set<AuthRole>
)

data class AuthenticatedSubjectResponse(
    val sub: String,
    val username: String,
    val roles: Set<AuthRole>
)
