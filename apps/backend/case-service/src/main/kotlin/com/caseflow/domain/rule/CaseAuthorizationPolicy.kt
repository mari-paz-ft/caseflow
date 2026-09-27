package com.caseflow.domain.rule

import com.caseflow.domain.exception.ForbiddenException
import com.caseflow.domain.model.CaseActor
import com.caseflow.domain.model.CaseRequest

object CaseAuthorizationPolicy {
    fun requireReadAccess(caseRequest: CaseRequest, actor: CaseActor) {
        if (!actor.isAdmin && caseRequest.ownerSubject != actor.subject) {
            throw ForbiddenException("Você não tem permissão para acessar esta solicitação")
        }
    }

    fun requireOwner(caseRequest: CaseRequest, actor: CaseActor) {
        if (caseRequest.ownerSubject != actor.subject) {
            throw ForbiddenException("Apenas o autor pode alterar esta solicitação")
        }
    }

    fun requireAdmin(actor: CaseActor) {
        if (!actor.isAdmin) {
            throw ForbiddenException("Apenas administradores podem solicitar reprocessamento")
        }
    }
}
