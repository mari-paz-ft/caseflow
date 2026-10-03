import {
  CaseRequest,
  CaseDocument,
  CaseHistory,
  NotificationItem,
  User,
  CreateCasePayload,
  DocumentCategory
} from '../types';

export const USERS: User[] = [
  {
    id: '11111111-1111-1111-1111-111111111111',
    email: 'solicitante@caseflow.local',
    fullName: 'Carlos Silva',
    role: 'ROLE_USER'
  },
  {
    id: '99999999-9999-9999-9999-999999999999',
    email: 'admin@caseflow.local',
    fullName: 'Mariana Paz',
    role: 'ROLE_ADMIN'
  }
];

// Initial mock data mirroring backend seeder
export const INITIAL_CASES: CaseRequest[] = [
  {
    id: 'a1111111-0000-0000-0000-000000000001',
    protocol: 'CF-20260922-1001',
    ownerSubject: '11111111-1111-1111-1111-111111111111',
    ownerEmail: 'solicitante@caseflow.local',
    title: 'Cadastro de Fornecedor - Alpha Tech',
    description: 'Solicitação inicial para conferência de documentos de credenciamento do fornecedor Alpha Tech.',
    caseType: 'ANALISE_DOCUMENTAL',
    status: 'RASCUNHO',
    version: 2,
    processingRun: 0,
    rulesVersion: 'DOCUMENTAL_V1',
    submittedAt: null,
    createdAt: new Date(Date.now() - 3600000 * 24).toISOString(),
    updatedAt: new Date(Date.now() - 3600000 * 5).toISOString(),
    documents: [
      {
        id: 'd1111111-0001',
        category: 'IDENTIFICACAO',
        fileName: 'contrato_social_rg.pdf',
        fileSize: 1048576,
        contentType: 'application/pdf',
        validUntil: new Date(Date.now() + 365 * 24 * 3600000).toISOString().split('T')[0],
        uploadState: 'READY',
        createdAt: new Date(Date.now() - 3600000 * 23).toISOString()
      }
    ],
    latestResult: null
  },
  {
    id: 'a2222222-0000-0000-0000-000000000002',
    protocol: 'CF-20260922-1002',
    ownerSubject: '11111111-1111-1111-1111-111111111111',
    ownerEmail: 'solicitante@caseflow.local',
    title: 'Validação Cadastral - Beta Consultoria',
    description: 'Submissão de comprovantes e identificação completa para validação cadastral anual.',
    caseType: 'ANALISE_DOCUMENTAL',
    status: 'APROVADA',
    version: 4,
    processingRun: 1,
    rulesVersion: 'DOCUMENTAL_V1',
    submittedAt: new Date(Date.now() - 3600000 * 3).toISOString(),
    createdAt: new Date(Date.now() - 3600000 * 4).toISOString(),
    updatedAt: new Date(Date.now() - 3600000 * 3).toISOString(),
    documents: [
      {
        id: 'd2222222-0001',
        category: 'IDENTIFICACAO',
        fileName: 'cnh_diretor.pdf',
        fileSize: 2097152,
        contentType: 'application/pdf',
        validUntil: new Date(Date.now() + 730 * 24 * 3600000).toISOString().split('T')[0],
        uploadState: 'READY',
        createdAt: new Date(Date.now() - 3600000 * 4).toISOString()
      },
      {
        id: 'd2222222-0002',
        category: 'COMPROVANTE_ENDERECO',
        fileName: 'comprovante_energia.pdf',
        fileSize: 850000,
        contentType: 'application/pdf',
        validUntil: new Date(Date.now() + 180 * 24 * 3600000).toISOString().split('T')[0],
        uploadState: 'READY',
        createdAt: new Date(Date.now() - 3600000 * 4).toISOString()
      }
    ],
    latestResult: {
      id: 'r2222222-0001',
      runNumber: 1,
      decision: 'APROVADA',
      reasonCodes: [],
      rulesVersion: 'DOCUMENTAL_V1',
      evaluatedAt: new Date(Date.now() - 3600000 * 3 + 4000).toISOString()
    }
  },
  {
    id: 'a3333333-0000-0000-0000-000000000003',
    protocol: 'CF-20260922-1003',
    ownerSubject: '11111111-1111-1111-1111-111111111111',
    ownerEmail: 'solicitante@caseflow.local',
    title: 'Atualização de Registro - Gama Logística',
    description: 'Atualização cadastral sem apresentação de comprovante de domicílio recente.',
    caseType: 'ANALISE_DOCUMENTAL',
    status: 'REJEITADA',
    version: 3,
    processingRun: 1,
    rulesVersion: 'DOCUMENTAL_V1',
    submittedAt: new Date(Date.now() - 3600000).toISOString(),
    createdAt: new Date(Date.now() - 3600000 * 2).toISOString(),
    updatedAt: new Date(Date.now() - 3600000).toISOString(),
    documents: [
      {
        id: 'd3333333-0001',
        category: 'IDENTIFICACAO',
        fileName: 'rg_frente_verso.pdf',
        fileSize: 1200000,
        contentType: 'application/pdf',
        validUntil: new Date(Date.now() + 365 * 24 * 3600000).toISOString().split('T')[0],
        uploadState: 'READY',
        createdAt: new Date(Date.now() - 3600000 * 2).toISOString()
      }
    ],
    latestResult: {
      id: 'r3333333-0001',
      runNumber: 1,
      decision: 'REJEITADA',
      reasonCodes: ['FALTA_COMPROVANTE_ENDERECO'],
      rulesVersion: 'DOCUMENTAL_V1',
      evaluatedAt: new Date(Date.now() - 3600000 + 2000).toISOString()
    }
  },
  {
    id: 'a4444444-0000-0000-0000-000000000004',
    protocol: 'CF-20260922-1004',
    ownerSubject: '11111111-1111-1111-1111-111111111111',
    ownerEmail: 'solicitante@caseflow.local',
    title: 'Credenciamento Urgente - Delta Distribuidora [SIMULAR_FALHA]',
    description: 'Solicitação urgente com arquivos anexados que sofreu interrupção durante a checagem no disco.',
    caseType: 'ANALISE_DOCUMENTAL',
    status: 'FALHA_TECNICA',
    version: 3,
    processingRun: 1,
    rulesVersion: 'DOCUMENTAL_V1',
    submittedAt: new Date(Date.now() - 1500000).toISOString(),
    createdAt: new Date(Date.now() - 1800000).toISOString(),
    updatedAt: new Date(Date.now() - 1500000).toISOString(),
    documents: [
      {
        id: 'd4444444-0001',
        category: 'IDENTIFICACAO',
        fileName: 'estatuto_social.pdf',
        fileSize: 1500000,
        contentType: 'application/pdf',
        validUntil: new Date(Date.now() + 365 * 24 * 3600000).toISOString().split('T')[0],
        uploadState: 'READY',
        createdAt: new Date(Date.now() - 1800000).toISOString()
      }
    ],
    latestResult: {
      id: 'r4444444-0001',
      runNumber: 1,
      decision: 'FALHA_TECNICA',
      reasonCodes: ['FALHA_INFRAESTRUTURA'],
      rulesVersion: 'DOCUMENTAL_V1',
      evaluatedAt: new Date(Date.now() - 1500000 + 1000).toISOString()
    }
  }
];

export const INITIAL_HISTORIES: Record<string, CaseHistory[]> = {
  'a1111111-0000-0000-0000-000000000001': [
    {
      id: 'h1',
      caseId: 'a1111111-0000-0000-0000-000000000001',
      eventType: 'DOCUMENTO_ANEXADO',
      actorSubject: 'solicitante@caseflow.local',
      details: 'Anexado arquivo contrato_social_rg.pdf',
      occurredAt: new Date(Date.now() - 3600000 * 23).toISOString()
    },
    {
      id: 'h2',
      caseId: 'a1111111-0000-0000-0000-000000000001',
      eventType: 'CRIACAO_RASCUNHO',
      actorSubject: 'solicitante@caseflow.local',
      details: 'Rascunho criado com protocolo CF-20260922-1001',
      occurredAt: new Date(Date.now() - 3600000 * 24).toISOString()
    }
  ],
  'a2222222-0000-0000-0000-000000000002': [
    {
      id: 'h3',
      caseId: 'a2222222-0000-0000-0000-000000000002',
      eventType: 'ANALISE_APROVADA',
      actorSubject: 'SYSTEM',
      details: 'Todos os critérios atendidos na versão DOCUMENTAL_V1',
      occurredAt: new Date(Date.now() - 3600000 * 3 + 4000).toISOString()
    },
    {
      id: 'h4',
      caseId: 'a2222222-0000-0000-0000-000000000002',
      eventType: 'SOLICITACAO_ENVIADA',
      actorSubject: 'solicitante@caseflow.local',
      details: 'Solicitação enviada para conferência documental (Run #1)',
      occurredAt: new Date(Date.now() - 3600000 * 3).toISOString()
    }
  ],
  'a3333333-0000-0000-0000-000000000003': [
    {
      id: 'h5',
      caseId: 'a3333333-0000-0000-0000-000000000003',
      eventType: 'ANALISE_REJEITADA',
      actorSubject: 'SYSTEM',
      details: 'Pendências identificadas: FALTA_COMPROVANTE_ENDERECO',
      occurredAt: new Date(Date.now() - 3600000 + 2000).toISOString()
    },
    {
      id: 'h6',
      caseId: 'a3333333-0000-0000-0000-000000000003',
      eventType: 'SOLICITACAO_ENVIADA',
      actorSubject: 'solicitante@caseflow.local',
      details: 'Solicitação enviada para conferência documental (Run #1)',
      occurredAt: new Date(Date.now() - 3600000).toISOString()
    }
  ],
  'a4444444-0000-0000-0000-000000000004': [
    {
      id: 'h7',
      caseId: 'a4444444-0000-0000-0000-000000000004',
      eventType: 'FALHA_TECNICA_REGISTRADA',
      actorSubject: 'SYSTEM',
      details: 'Simulação de indisponibilidade de I/O de armazenamento',
      occurredAt: new Date(Date.now() - 1500000 + 1000).toISOString()
    },
    {
      id: 'h8',
      caseId: 'a4444444-0000-0000-0000-000000000004',
      eventType: 'SOLICITACAO_ENVIADA',
      actorSubject: 'solicitante@caseflow.local',
      details: 'Solicitação enviada para conferência documental (Run #1)',
      occurredAt: new Date(Date.now() - 1500000).toISOString()
    }
  ]
};

export const INITIAL_NOTIFICATIONS: NotificationItem[] = [
  {
    id: 'n1',
    caseId: 'a2222222-0000-0000-0000-000000000002',
    title: 'Solicitação CF-20260922-1002 Aprovada',
    message: 'Sua documentação foi conferida com sucesso e aprovada pelo motor de regras.',
    readAt: null,
    createdAt: new Date(Date.now() - 3600000 * 3).toISOString()
  },
  {
    id: 'n2',
    caseId: 'a3333333-0000-0000-0000-000000000003',
    title: 'Solicitação CF-20260922-1003 Rejeitada',
    message: 'Sua solicitação foi rejeitada pelos seguintes motivos: FALTA_COMPROVANTE_ENDERECO.',
    readAt: null,
    createdAt: new Date(Date.now() - 3600000).toISOString()
  },
  {
    id: 'n3',
    caseId: 'a4444444-0000-0000-0000-000000000004',
    title: 'Instabilidade no processamento de CF-20260922-1004',
    message: 'Ocorreu uma falha técnica durante a conferência. Um administrador foi notificado.',
    readAt: null,
    createdAt: new Date(Date.now() - 1500000).toISOString()
  }
];

