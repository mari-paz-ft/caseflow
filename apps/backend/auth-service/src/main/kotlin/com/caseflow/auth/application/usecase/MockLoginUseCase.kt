package com.caseflow.auth.application.usecase

import com.caseflow.auth.application.dto.LoginRequest
import com.caseflow.auth.application.dto.LoginResponse
import com.caseflow.auth.application.port.TokenIssuer
import com.caseflow.auth.domain.model.AuthRole
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID

@Service
class MockLoginUseCase(
    private val tokenIssuer: TokenIssuer
) {
    fun login(request: LoginRequest): LoginResponse {
        val role = if (request.username.contains("admin", ignoreCase = true)) AuthRole.ADMIN else AuthRole.USER
        val subject = UUID.nameUUIDFromBytes(request.username.toByteArray(UTF_8)).toString()
        val issuedToken = tokenIssuer.issue(subject, request.username, setOf(role))
        return LoginResponse(
            accessToken = issuedToken.value,
            tokenType = "Bearer",
            expiresAt = issuedToken.expiresAt,
            sub = subject,
            username = request.username,
            roles = setOf(role)
        )
    }
}
