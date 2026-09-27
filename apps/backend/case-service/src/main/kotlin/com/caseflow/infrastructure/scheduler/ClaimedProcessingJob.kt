package com.caseflow.infrastructure.scheduler

import java.util.UUID

data class ClaimedProcessingJob(
    val jobId: UUID,
    val leaseToken: UUID
)
