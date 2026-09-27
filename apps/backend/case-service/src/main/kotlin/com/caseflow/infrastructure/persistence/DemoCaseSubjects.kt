package com.caseflow.infrastructure.persistence

import java.nio.charset.StandardCharsets.UTF_8
import java.util.UUID

object DemoCaseSubjects {
    const val USERNAME = "solicitante@caseflow.local"
    const val ADMIN_USERNAME = "admin@caseflow.local"

    fun fromUsername(username: String): UUID = UUID.nameUUIDFromBytes(username.toByteArray(UTF_8))
}
