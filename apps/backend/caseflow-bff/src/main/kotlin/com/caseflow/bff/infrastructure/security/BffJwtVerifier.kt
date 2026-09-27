package com.caseflow.bff.infrastructure.security

import com.caseflow.bff.domain.model.BffPrincipal
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.time.Clock
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class BffJwtVerifier(
    @Value("\${caseflow.jwt.secret}") secret: String,
    private val objectMapper: ObjectMapper,
    private val clock: Clock
) {
    private val key = SecretKeySpec(secret.toByteArray(UTF_8), SIGNING_ALGORITHM)

    init {
        require(secret.toByteArray(UTF_8).size >= 32) { "JWT HMAC secret must contain at least 32 bytes" }
    }

    fun authenticate(token: String): BffPrincipal? = runCatching {
        val parts = token.split('.')
        if (parts.size != 3) return null

        val header = objectMapper.readTree(decode(parts[0]))
        if (header.path("alg").asText() != JWT_ALGORITHM) return null
        if (!MessageDigest.isEqual(sign("${parts[0]}.${parts[1]}"), decode(parts[2]))) return null

        val claims = objectMapper.readTree(decode(parts[1]))
        if (claims.path("iss").asText() != ISSUER) return null
        val subject = claims.path("sub").asText().takeIf(String::isNotBlank) ?: return null
        UUID.fromString(subject)
        val username = claims.path("username").asText().takeIf(String::isNotBlank) ?: return null

        val issuedAtClaim = claims.path("iat")
        val expiresAtClaim = claims.path("exp")
        if (!issuedAtClaim.isIntegralNumber || !expiresAtClaim.isIntegralNumber) return null
        val issuedAt = issuedAtClaim.asLong()
        val expiresAt = expiresAtClaim.asLong()
        val now = clock.instant().epochSecond
        if (issuedAt > now || expiresAt <= now || expiresAt <= issuedAt) return null

        val rolesClaim = claims.path("roles")
        if (!rolesClaim.isArray || rolesClaim.isEmpty) return null
        val roles = rolesClaim.map { it.asText() }.toSet()
        if (roles.any { it != "USER" && it != "ADMIN" }) return null
        BffPrincipal(subject, username, roles)
    }.getOrNull()

    private fun sign(value: String): ByteArray = Mac.getInstance(SIGNING_ALGORITHM).run {
        init(key)
        doFinal(value.toByteArray(UTF_8))
    }

    private fun decode(value: String): ByteArray = Base64.getUrlDecoder().decode(value)

    companion object {
        private const val JWT_ALGORITHM = "HS256"
        private const val SIGNING_ALGORITHM = "HmacSHA256"
        private const val ISSUER = "caseflow-auth-service"
    }
}
