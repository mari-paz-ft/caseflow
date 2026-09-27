package com.caseflow.application.port

import com.caseflow.domain.model.CaseActor

interface CurrentCaseActorProvider {
    fun currentActor(): CaseActor
}
