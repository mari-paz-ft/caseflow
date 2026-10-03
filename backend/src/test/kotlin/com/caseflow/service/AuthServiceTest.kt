package com.caseflow.service

import com.caseflow.controller.dto.LoginRequestDto
import com.caseflow.domain.enums.RoleName
import com.caseflow.domain.exception.ResourceNotFoundException
import com.caseflow.domain.model.AppUser
import com.caseflow.repository.AppUserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional
import java.util.UUID

class AuthServiceTest {
    private val user = AppUser(
        id = UUID.fromString("11111111-1111-1111-1111-111111111111"),
        email = "solicitante@caseflow.local",
        fullName = "Carlos Silva",
        passwordHash = "{bcrypt-hash}",
        role = RoleName.ROLE_USER
    )
    private val repository = mock(AppUserRepository::class.java)
    private val passwordEncoder = mock(PasswordEncoder::class.java)
    private val authService = AuthService(repository, passwordEncoder)

    @Test
    fun `login returns token and user when password matches`() {
        `when`(repository.findByEmail(user.email)).thenReturn(Optional.of(user))
        `when`(passwordEncoder.matches("senha123", user.passwordHash)).thenReturn(true)

        val result = authService.login(LoginRequestDto(user.email, "senha123"))

        assertEquals("mock-token-${user.email}", result.token)
        assertEquals(user.email, result.user.email)
    }

    @Test
    fun `login rejects incorrect password`() {
        `when`(repository.findByEmail(user.email)).thenReturn(Optional.of(user))
        `when`(passwordEncoder.matches("errada", user.passwordHash)).thenReturn(false)

        assertThrows(ResourceNotFoundException::class.java) {
            authService.login(LoginRequestDto(user.email, "errada"))
        }
    }
}
