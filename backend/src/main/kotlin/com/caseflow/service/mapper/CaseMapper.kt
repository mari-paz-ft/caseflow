package com.caseflow.service.mapper

import com.caseflow.controller.dto.CaseDocumentDto
import com.caseflow.controller.dto.CaseResponseDto
import com.caseflow.controller.dto.ProcessingResultDto
import com.caseflow.domain.model.CaseDocument
import com.caseflow.domain.model.CaseRequest
import com.caseflow.domain.model.ProcessingResult
import com.caseflow.repository.ProcessingResultRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

@Component
class CaseMapper(
    private val processingResultRepository: ProcessingResultRepository,
    private val objectMapper: ObjectMapper
) {

    fun toDto(caseRequest: CaseRequest): CaseResponseDto {
        val latestResult = processingResultRepository.findFirstByCaseRequestIdOrderByRunNumberDesc(caseRequest.id)
            .map { toProcessingResultDto(it) }
            .orElse(null)

        val docDtos = caseRequest.documents.map { toDocumentDto(it) }

        return CaseResponseDto(
            id = caseRequest.id,
            protocol = caseRequest.protocol,
            ownerSubject = caseRequest.ownerSubject,
            ownerEmail = caseRequest.ownerEmail,
            title = caseRequest.title,
            description = caseRequest.description,
            caseType = caseRequest.caseType,
            status = caseRequest.status,
            version = caseRequest.version,
            processingRun = caseRequest.processingRun,
            rulesVersion = caseRequest.rulesVersion,
            submittedAt = caseRequest.submittedAt,
            createdAt = caseRequest.createdAt,
            updatedAt = caseRequest.updatedAt,
            documents = docDtos,
            latestResult = latestResult
        )
    }

    fun toDocumentDto(doc: CaseDocument): CaseDocumentDto {
        return CaseDocumentDto(
            id = doc.id,
            category = doc.category,
            fileName = doc.fileName,
            fileSize = doc.fileSize,
            contentType = doc.contentType,
            validUntil = doc.validUntil,
            uploadState = doc.uploadState,
            createdAt = doc.createdAt
        )
    }

    fun toProcessingResultDto(res: ProcessingResult): ProcessingResultDto {
        val reasonList: List<String> = try {
            objectMapper.readValue(res.reasonCodes, object : TypeReference<List<String>>() {})
        } catch (e: Exception) {
            emptyList()
        }
        return ProcessingResultDto(
            id = res.id,
            runNumber = res.runNumber,
            decision = res.decision,
            reasonCodes = reasonList,
            rulesVersion = res.rulesVersion,
            evaluatedAt = res.evaluatedAt
        )
    }
}
