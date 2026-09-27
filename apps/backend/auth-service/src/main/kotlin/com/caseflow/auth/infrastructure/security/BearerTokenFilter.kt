package com.caseflow.auth.infrastructure.security

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class BearerTokenFilter(
    private val jwtTokenService: HmacJwtTokenService
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authorization = request.getHeader("Authorization")
        if (authorization == null) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authorization.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")
        val subject = token?.let(jwtTokenService::authenticate)
        if (subject == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            return
        }

        val authorities = subject.roles.map { SimpleGrantedAuthority("ROLE_${it.name}") }
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(subject, null, authorities)
        filterChain.doFilter(request, response)
    }
}
