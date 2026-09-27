package com.caseflow.domain.rule

object TechnicalRetryPolicy {
    fun retryDelaySeconds(failureCount: Int): Long? = when (failureCount) {
        1 -> 10
        2 -> 30
        else -> null
    }
}
