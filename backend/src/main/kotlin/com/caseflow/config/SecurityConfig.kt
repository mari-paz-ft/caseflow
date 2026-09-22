package com.caseflow.config

import com.caseflow.domain.enums.RoleName
import com.caseflow.domain.model.AppUser
import com.caseflow.repository.AppUserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.OncePerRequestFilter

class CurrentUserContext {
    companion object {
        private val userThreadLocal = ThreadLocal<AppUser>()

        fun get(): AppUser? = userThreadLocal.get()
        fun set(user: AppUser) = userThreadLocal.set(user)
        fun clear() = userThreadLocal.remove()
    }
}

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
            else -> "solicitante@caseflow.local" // default user for easy testing
        }

        val userOpt = appUserRepository.findByEmail(targetEmail)
        if (userOpt.isPresent) {
            val user = userOpt.get()
            CurrentUserContext.set(user)

            val authorities = listOf(SimpleGrantedAuthority(user.role.name))
            val auth = UsernamePasswordAuthenticationToken(user, null, authorities)
            SecurityContextHolder.getContext().authentication = auth
        }

        try {
            filterChain.doFilter(request, response)
        } finally {
            CurrentUserContext.clear()
        }
    }
}

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val appUserRepository: AppUserRepository
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/h2-console/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/api/v1/auth/**",
                        "/bff/v1/csrf",
                        "/bff/v1/me"
                    ).permitAll()
                    .anyRequest().permitAll() // Authorizations checked in services/controllers for MVP flexibility
            }
            .headers { headers ->
                headers.frameOptions { it.disable() } // For H2 console
            }
            .addFilterBefore(HeaderAuthFilter(appUserRepository), UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOriginPatterns = listOf("*")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.exposedHeaders = listOf("X-Case-Version", "Location")

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
