package com.caseflow.auth.interfaces.rest

import com.caseflow.auth.application.dto.AuthenticatedSubjectResponse
import com.caseflow.auth.application.dto.LoginRequest
import com.caseflow.auth.application.dto.LoginResponse
import com.caseflow.auth.application.usecase.MockLoginUseCase
import com.caseflow.auth.domain.model.AuthenticatedSubject
import jakarta.validation.Valid
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val mockLoginUseCase: MockLoginUseCase
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse = mockLoginUseCase.login(request)

    @GetMapping("/me")
    fun me(authentication: Authentication): AuthenticatedSubjectResponse {
        val subject = authentication.principal as AuthenticatedSubject
        return AuthenticatedSubjectResponse(subject.sub, subject.username, subject.roles)
    }
}
