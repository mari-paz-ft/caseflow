package com.caseflow.bff.interfaces.rest

import com.caseflow.bff.application.dto.BffLoginRequest
import com.caseflow.bff.application.dto.BffLoginResponse
import com.caseflow.bff.application.usecase.LoginUseCase
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val loginUseCase: LoginUseCase
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: BffLoginRequest): BffLoginResponse = loginUseCase.login(request)
}
