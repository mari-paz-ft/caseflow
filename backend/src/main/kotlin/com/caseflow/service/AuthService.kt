package com.caseflow.service

import com.caseflow.config.CurrentUserContext
import com.caseflow.controller.dto.LoginRequestDto
import com.caseflow.controller.dto.LoginResponseDto
import com.caseflow.controller.dto.UserDto
import com.caseflow.domain.exception.ResourceNotFoundException
import com.caseflow.domain.model.AppUser
import com.caseflow.repository.AppUserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val appUserRepository: AppUserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional(readOnly = true)
    fun getCurrentUser(): AppUser {
        return CurrentUserContext.get()
            ?: appUserRepository.findByEmail("solicitante@caseflow.local").orElseThrow {
                ResourceNotFoundException("Usuário padrão não encontrado")
            }
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequestDto): LoginResponseDto {
        val user = appUserRepository.findByEmail(request.email)
            .orElseThrow { ResourceNotFoundException("Usuário ou senha incorretos") }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw ResourceNotFoundException("Usuário ou senha incorretos")
        }

        val token = "mock-token-${user.email}"
        val userDto = UserDto(user.id, user.email, user.fullName, user.role)
        return LoginResponseDto(token, userDto)
    }
}
