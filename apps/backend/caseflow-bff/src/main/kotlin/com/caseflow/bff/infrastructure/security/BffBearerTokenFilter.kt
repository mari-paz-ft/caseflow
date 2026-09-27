package com.caseflow.bff.infrastructure.security

import com.caseflow.bff.domain.model.BffPrincipal
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class BffBearerTokenFilter(
    private val jwtVerifier: BffJwtVerifier
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
        val principal = token?.let(jwtVerifier::authenticate)
        if (principal == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            return
        }

        val authorities = principal.roles.map { SimpleGrantedAuthority("ROLE_$it") }
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, authorities)
        filterChain.doFilter(request, response)
    }
}
