package com.caseflow.infrastructure.security

import com.caseflow.application.port.CurrentCaseActorProvider
import com.caseflow.domain.model.CaseActor
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class SecurityCaseActorProvider : CurrentCaseActorProvider {
    override fun currentActor(): CaseActor =
        SecurityContextHolder.getContext().authentication?.principal as? CaseActor
            ?: throw IllegalStateException("JWT authentication is required")
}
