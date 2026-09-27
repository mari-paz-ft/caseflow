package com.caseflow.auth.infrastructure.security

import com.caseflow.auth.application.port.IssuedToken
import com.caseflow.auth.application.port.TokenIssuer
import com.caseflow.auth.domain.model.AuthRole
import com.caseflow.auth.domain.model.AuthenticatedSubject
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.time.Clock
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class HmacJwtTokenService(
    @Value("\${caseflow.jwt.secret}") secret: String,
    @Value("\${caseflow.jwt.ttl-seconds:3600}") private val ttlSeconds: Long,
    private val objectMapper: ObjectMapper,
    private val clock: Clock
) : TokenIssuer {
    private val key = SecretKeySpec(secret.toByteArray(UTF_8), SIGNING_ALGORITHM)

    init {
        require(secret.toByteArray(UTF_8).size >= 32) { "JWT HMAC secret must contain at least 32 bytes" }
        require(ttlSeconds > 0) { "JWT TTL must be positive" }
    }

    override fun issue(subject: String, username: String, roles: Set<AuthRole>): IssuedToken {
        val issuedAt = clock.instant()
        val expiresAt = issuedAt.plusSeconds(ttlSeconds)
        val header = mapOf("alg" to JWT_ALGORITHM, "typ" to "JWT")
        val claims = mapOf(
            "sub" to subject,
            "username" to username,
            "roles" to roles.map(AuthRole::name),
            "iat" to issuedAt.epochSecond,
            "exp" to expiresAt.epochSecond,
            "iss" to ISSUER
        )
        val encodedHeader = encode(objectMapper.writeValueAsBytes(header))
        val encodedClaims = encode(objectMapper.writeValueAsBytes(claims))
        val signingInput = "$encodedHeader.$encodedClaims"
        val signature = encode(sign(signingInput))
        return IssuedToken("$signingInput.$signature", expiresAt)
    }

    fun authenticate(token: String): AuthenticatedSubject? = runCatching {
        val parts = token.split('.')
        if (parts.size != 3) return null

        val header = objectMapper.readTree(decode(parts[0]))
        if (header.path("alg").asText() != JWT_ALGORITHM) return null

        val signingInput = "${parts[0]}.${parts[1]}"
        val providedSignature = decode(parts[2])
        if (!MessageDigest.isEqual(sign(signingInput), providedSignature)) return null

        val claims = objectMapper.readTree(decode(parts[1]))
        val subject = claims.path("sub").asText().takeIf(String::isNotBlank) ?: return null
        val username = claims.path("username").asText().takeIf(String::isNotBlank) ?: return null
        if (claims.path("iss").asText() != ISSUER) return null

        val issuedAtClaim = claims.path("iat")
        val expiresAtClaim = claims.path("exp")
        if (!issuedAtClaim.isIntegralNumber || !expiresAtClaim.isIntegralNumber) return null

        val issuedAt = issuedAtClaim.asLong()
        val expiresAt = expiresAtClaim.asLong()
        val now = clock.instant().epochSecond
        if (issuedAt > now || expiresAt <= now || expiresAt <= issuedAt) return null

        val rolesClaim = claims.path("roles")
        if (!rolesClaim.isArray || rolesClaim.isEmpty) return null
        val roles = rolesClaim.map { AuthRole.entries.firstOrNull { role -> role.name == it.asText() } ?: return null }.toSet()

        AuthenticatedSubject(subject, username, roles)
    }.getOrNull()

    private fun sign(value: String): ByteArray = Mac.getInstance(SIGNING_ALGORITHM).run {
        init(key)
        doFinal(value.toByteArray(UTF_8))
    }

    private fun encode(value: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(value)

    private fun decode(value: String): ByteArray = Base64.getUrlDecoder().decode(value)

    companion object {
        private const val JWT_ALGORITHM = "HS256"
        private const val SIGNING_ALGORITHM = "HmacSHA256"
        private const val ISSUER = "caseflow-auth-service"
    }
}
