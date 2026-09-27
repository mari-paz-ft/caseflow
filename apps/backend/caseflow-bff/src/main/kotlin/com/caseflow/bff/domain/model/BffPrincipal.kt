package com.caseflow.bff.domain.model

data class BffPrincipal(
    val sub: String,
    val username: String,
    val roles: Set<String>
)
