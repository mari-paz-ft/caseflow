package com.caseflow.infrastructure.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class CaseJwtAuthenticationFilter(
    private val jwtVerifier: HmacJwtVerifier
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
        val actor = token?.let(jwtVerifier::authenticate)
        if (actor == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            return
        }

        val authorities = actor.roles.map { SimpleGrantedAuthority(it.name) }
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(actor, null, authorities)
        filterChain.doFilter(request, response)
    }
}
