package com.caseflow.domain.model

import com.caseflow.domain.enums.RoleName
import java.util.UUID

data class CaseActor(
    val subject: UUID,
    val username: String,
    val roles: Set<RoleName>
) {
    val isAdmin: Boolean
        get() = RoleName.ROLE_ADMIN in roles
}
