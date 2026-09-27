package com.caseflow.domain.exception

enum class CaseFlowErrorKind {
    BAD_REQUEST,
    NOT_FOUND,
    FORBIDDEN,
    CONFLICT
}

open class CaseFlowException(
    override val message: String,
    val kind: CaseFlowErrorKind = CaseFlowErrorKind.BAD_REQUEST,
    val errorCode: String = "BUSINESS_ERROR"
) : RuntimeException(message)

class ResourceNotFoundException(message: String) :
    CaseFlowException(message, CaseFlowErrorKind.NOT_FOUND, "RESOURCE_NOT_FOUND")

class ForbiddenException(message: String) :
    CaseFlowException(message, CaseFlowErrorKind.FORBIDDEN, "FORBIDDEN_ACCESS")

class ConflictException(message: String, errorCode: String = "CONFLICT_STATE") :
    CaseFlowException(message, CaseFlowErrorKind.CONFLICT, errorCode)

class TechnicalFailureException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
