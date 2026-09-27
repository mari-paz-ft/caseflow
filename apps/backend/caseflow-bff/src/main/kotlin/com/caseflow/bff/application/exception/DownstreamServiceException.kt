package com.caseflow.bff.application.exception

class DownstreamServiceException(
    val statusCode: Int,
    val errorCode: String,
    override val message: String,
    val traceId: String?
) : RuntimeException(message)
