package com.caseflow.service

import com.caseflow.domain.enums.*
import com.caseflow.domain.model.*
import com.caseflow.repository.*
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class DataSeederService(
    private val appUserRepository: AppUserRepository,
    private val caseRequestRepository: CaseRequestRepository,
    private val caseDocumentRepository: CaseDocumentRepository,
    private val processingResultRepository: ProcessingResultRepository,
    private val caseHistoryRepository: CaseHistoryRepository,
    private val notificationRepository: NotificationRepository
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(DataSeederService::class.java)

    @Transactional
    override fun run(vararg args: String?) {
        if (appUserRepository.count() > 0) {
            return
        }
        logger.info("Populando dados mockados iniciais do CaseFlow...")

        val userSubject = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val adminSubject = UUID.fromString("99999999-9999-9999-9999-999999999999")

        val solicitante = AppUser(
            id = userSubject,
            email = "solicitante@caseflow.local",
            fullName = "Carlos Silva (Solicitante)",
            passwordHash = "senha123",
            role = RoleName.ROLE_USER
        )

        val admin = AppUser(
            id = adminSubject,
            email = "admin@caseflow.local",
            fullName = "Mariana Paz (Administradora)",
            passwordHash = "admin123",
            role = RoleName.ROLE_ADMIN
        )

        appUserRepository.saveAll(listOf(solicitante, admin))

        // Caso 1: RASCUNHO (Pronto para anexar e enviar)
        val case1 = CaseRequest(
            id = UUID.fromString("a1111111-0000-0000-0000-000000000001"),
            protocol = "CF-20260922-1001",
            ownerSubject = userSubject,
            ownerEmail = solicitante.email,
            title = "Cadastro de Fornecedor - Alpha Tech",
            description = "Solicitação inicial para conferência de documentos de credenciamento do fornecedor Alpha Tech.",
            status = CaseStatus.RASCUNHO,
            version = 2,
            processingRun = 0,
            submittedAt = null
        )
        val doc1 = CaseDocument(
            id = UUID.randomUUID(),
            caseRequest = case1,
            category = DocumentCategory.IDENTIFICACAO,
            fileName = "contrato_social_rg.pdf",
            fileSize = 1048576,
            contentType = "application/pdf",
            storageKey = "case1_identificacao.pdf",
            sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            validUntil = LocalDate.now().plusYears(1),
            uploadState = UploadState.READY
        )
        case1.documents.add(doc1)
        caseRequestRepository.save(case1)

        caseHistoryRepository.save(
            CaseHistory(
                caseRequest = case1,
                eventType = "CRIACAO_RASCUNHO",
                actorSubject = solicitante.email,
                details = "Rascunho criado no portal"
            )
        )

        // Caso 2: APROVADA
        val case2 = CaseRequest(
            id = UUID.fromString("a2222222-0000-0000-0000-000000000002"),
            protocol = "CF-20260922-1002",
            ownerSubject = userSubject,
            ownerEmail = solicitante.email,
            title = "Validação Cadastral - Beta Consultoria",
            description = "Submissão de comprovantes e identificação completa para validação cadastral anual.",
            status = CaseStatus.APROVADA,
            version = 4,
            processingRun = 1,
            submittedAt = LocalDateTime.now().minusHours(3)
        )
        val doc2Id = CaseDocument(
            id = UUID.randomUUID(),
            caseRequest = case2,
            category = DocumentCategory.IDENTIFICACAO,
            fileName = "cnh_diretor.pdf",
            fileSize = 2097152,
            storageKey = "case2_cnh.pdf",
            sha256 = "c0535e4be2b79ffd93291305436bf889314e4a3faec05ecffcbb7ff310919046",
            validUntil = LocalDate.now().plusYears(2),
            uploadState = UploadState.READY
        )
        val doc2End = CaseDocument(
            id = UUID.randomUUID(),
            caseRequest = case2,
            category = DocumentCategory.COMPROVANTE_ENDERECO,
            fileName = "comprovante_energia.pdf",
            fileSize = 850000,
            storageKey = "case2_end.pdf",
            sha256 = "5d41402abc4b2a76b9719d911017c592",
            validUntil = LocalDate.now().plusMonths(6),
            uploadState = UploadState.READY
        )
        case2.documents.addAll(listOf(doc2Id, doc2End))
        caseRequestRepository.save(case2)

        processingResultRepository.save(
            ProcessingResult(
                caseRequest = case2,
                runNumber = 1,
                decision = ProcessingDecision.APROVADA,
                reasonCodes = "[]",
                rulesVersion = "DOCUMENTAL_V1",
                evaluatedAt = LocalDateTime.now().minusHours(3).plusSeconds(4)
            )
        )

        caseHistoryRepository.save(
            CaseHistory(
                caseRequest = case2,
                eventType = "ANALISE_APROVADA",
                actorSubject = "SYSTEM",
                details = "Todos os critérios atendidos na versão DOCUMENTAL_V1"
            )
        )

        notificationRepository.save(
            Notification(
                caseRequest = case2,
                recipientSubject = userSubject,
                title = "Solicitação ${case2.protocol} Aprovada",
                message = "Sua documentação foi conferida com sucesso e aprovada pelo motor de regras."
            )
        )

        // Caso 3: REJEITADA (Falta comprovante de endereço)
        val case3 = CaseRequest(
            id = UUID.fromString("a3333333-0000-0000-0000-000000000003"),
            protocol = "CF-20260922-1003",
            ownerSubject = userSubject,
            ownerEmail = solicitante.email,
            title = "Atualização de Registro - Gama Logística",
            description = "Atualização cadastral sem apresentação de comprovante de domicílio recente.",
            status = CaseStatus.REJEITADA,
            version = 3,
            processingRun = 1,
            submittedAt = LocalDateTime.now().minusHours(1)
        )
        val doc3Id = CaseDocument(
            id = UUID.randomUUID(),
            caseRequest = case3,
            category = DocumentCategory.IDENTIFICACAO,
            fileName = "rg_frente_verso.pdf",
            fileSize = 1200000,
            storageKey = "case3_rg.pdf",
            sha256 = "7d793037a0760186574b0282f2f435e7",
            validUntil = LocalDate.now().plusYears(5),
            uploadState = UploadState.READY
        )
        case3.documents.add(doc3Id)
        caseRequestRepository.save(case3)

        processingResultRepository.save(
            ProcessingResult(
                caseRequest = case3,
                runNumber = 1,
                decision = ProcessingDecision.REJEITADA,
                reasonCodes = "[\"FALTA_COMPROVANTE_ENDERECO\"]",
                rulesVersion = "DOCUMENTAL_V1",
                evaluatedAt = LocalDateTime.now().minusHours(1).plusSeconds(2)
            )
        )

        caseHistoryRepository.save(
            CaseHistory(
                caseRequest = case3,
                eventType = "ANALISE_REJEITADA",
                actorSubject = "SYSTEM",
                details = "Pendências identificadas: FALTA_COMPROVANTE_ENDERECO"
            )
        )

        notificationRepository.save(
            Notification(
                caseRequest = case3,
                recipientSubject = userSubject,
                title = "Solicitação ${case3.protocol} Rejeitada",
                message = "Sua solicitação foi rejeitada pelos seguintes motivos: FALTA_COMPROVANTE_ENDERECO."
            )
        )

        // Caso 4: FALHA TÉCNICA (Pronto para teste de Retry por ADMIN)
        val case4 = CaseRequest(
            id = UUID.fromString("a4444444-0000-0000-0000-000000000004"),
            protocol = "CF-20260922-1004",
            ownerSubject = userSubject,
            ownerEmail = solicitante.email,
            title = "Credenciamento Urgente - Delta Distribuidora [SIMULAR_FALHA]",
            description = "Solicitação urgente com arquivos anexados que sofreu interrupção durante a checagem no disco.",
            status = CaseStatus.FALHA_TECNICA,
            version = 3,
            processingRun = 1,
            submittedAt = LocalDateTime.now().minusMinutes(25)
        )
        val doc4Id = CaseDocument(
            id = UUID.randomUUID(),
            caseRequest = case4,
            category = DocumentCategory.IDENTIFICACAO,
            fileName = "estatuto_social.pdf",
            fileSize = 1500000,
            storageKey = "case4_estatuto.pdf",
            sha256 = "9b71d224bd62f3785d96d46ad3ea3d73319bf52da",
            validUntil = LocalDate.now().plusYears(3),
            uploadState = UploadState.READY
        )
        case4.documents.add(doc4Id)
        caseRequestRepository.save(case4)

        processingResultRepository.save(
            ProcessingResult(
                caseRequest = case4,
                runNumber = 1,
                decision = ProcessingDecision.FALHA_TECNICA,
                reasonCodes = "[\"FALHA_INFRAESTRUTURA\"]",
                rulesVersion = "DOCUMENTAL_V1",
                evaluatedAt = LocalDateTime.now().minusMinutes(24)
            )
        )

        caseHistoryRepository.save(
            CaseHistory(
                caseRequest = case4,
                eventType = "FALHA_TECNICA_REGISTRADA",
                actorSubject = "SYSTEM",
                details = "Simulação de indisponibilidade de I/O de armazenamento"
            )
        )

        logger.info("Dados mockados inseridos com sucesso: 2 usuários e 4 solicitações de teste.")
    }
}
