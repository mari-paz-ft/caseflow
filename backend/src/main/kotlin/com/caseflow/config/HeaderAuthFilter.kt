package com.caseflow.config

import com.caseflow.domain.model.AppUser
import com.caseflow.repository.AppUserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.filter.OncePerRequestFilter

class HeaderAuthFilter(
    private val appUserRepository: AppUserRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val emailHeader = request.getHeader("X-User-Email")
        val authHeader = request.getHeader("Authorization")
        val targetEmail = when {
            !emailHeader.isNullOrBlank() -> emailHeader
            authHeader?.startsWith("Bearer mock-token-") == true -> authHeader.substringAfter("Bearer mock-token-")
            else -> "solicitante@caseflow.local"
        }

        val userOptional = appUserRepository.findByEmail(targetEmail)
        if (userOptional.isPresent) {
            val user: AppUser = userOptional.get()
            CurrentUserContext.set(user)
            val authorities = listOf(SimpleGrantedAuthority(user.role.name))
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(user, null, authorities)
        }

        try {
            filterChain.doFilter(request, response)
        } finally {
            CurrentUserContext.clear()
        }
    }
}
