package com.caseflow.bff.application.usecase

import com.caseflow.bff.application.dto.BffLoginRequest
import com.caseflow.bff.application.dto.BffLoginResponse
import com.caseflow.bff.application.dto.BffUserDto
import com.caseflow.bff.application.port.AuthServiceClient
import org.springframework.stereotype.Service

@Service
class LoginUseCase(
    private val authServiceClient: AuthServiceClient
) {
    fun login(request: BffLoginRequest): BffLoginResponse {
        val identity = authServiceClient.login(request.username, request.password)
        val role = if ("ADMIN" in identity.roles) "ROLE_ADMIN" else "ROLE_USER"
        return BffLoginResponse(
            token = identity.accessToken,
            tokenType = identity.tokenType,
            expiresAt = identity.expiresAt,
            user = BffUserDto(
                id = identity.sub,
                email = identity.username,
                fullName = identity.username,
                role = role
            )
        )
    }
}
