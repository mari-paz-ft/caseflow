package com.caseflow.domain.enums

enum class CaseStatus {
    RASCUNHO,
    ENVIADA,
    PROCESSANDO,
    APROVADA,
    REJEITADA,
    FALHA_TECNICA
}

enum class DocumentCategory {
    IDENTIFICACAO,
    COMPROVANTE_ENDERECO,
    COMPLEMENTAR
}

enum class UploadState {
    PENDING,
    READY,
    FAILED
}

enum class JobState {
    SCHEDULED,
    RUNNING,
    COMPLETED,
    FAILED
}

enum class ProcessingDecision {
    APROVADA,
    REJEITADA,
    FALHA_TECNICA
}

enum class RoleName {
    ROLE_USER,
    ROLE_ADMIN
}
