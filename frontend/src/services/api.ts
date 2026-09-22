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
const INITIAL_CASES: CaseRequest[] = [
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

const INITIAL_HISTORIES: Record<string, CaseHistory[]> = {
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

const INITIAL_NOTIFICATIONS: NotificationItem[] = [
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

class StorageService {
  private cases: CaseRequest[];
  private histories: Record<string, CaseHistory[]>;
  private notifications: NotificationItem[];

  constructor() {
    const storedCases = localStorage.getItem('caseflow_cases');
    const storedHistories = localStorage.getItem('caseflow_histories');
    const storedNotifs = localStorage.getItem('caseflow_notifs');

    this.cases = storedCases ? JSON.parse(storedCases) : INITIAL_CASES;
    this.histories = storedHistories ? JSON.parse(storedHistories) : INITIAL_HISTORIES;
    this.notifications = storedNotifs ? JSON.parse(storedNotifs) : INITIAL_NOTIFICATIONS;
  }

  save() {
    localStorage.setItem('caseflow_cases', JSON.stringify(this.cases));
    localStorage.setItem('caseflow_histories', JSON.stringify(this.histories));
    localStorage.setItem('caseflow_notifs', JSON.stringify(this.notifications));
  }

  reset() {
    this.cases = INITIAL_CASES;
    this.histories = INITIAL_HISTORIES;
    this.notifications = INITIAL_NOTIFICATIONS;
    this.save();
  }

  getCases(user: User): CaseRequest[] {
    if (user.role === 'ROLE_ADMIN') {
      return [...this.cases];
    }
    return this.cases.filter(c => c.ownerSubject === user.id);
  }

  getCaseById(id: string, user: User): CaseRequest | null {
    const found = this.cases.find(c => c.id === id);
    if (!found) return null;
    if (user.role !== 'ROLE_ADMIN' && found.ownerSubject !== user.id) {
      throw new Error('Acesso negado');
    }
    return found;
  }

  createCase(payload: CreateCasePayload, user: User): CaseRequest {
    const id = 'case-' + Math.random().toString(36).substring(2, 9);
    const datePart = new Date().toISOString().slice(0, 10).replace(/-/g, '');
    const randPart = Math.floor(1000 + Math.random() * 9000);
    const protocol = `CF-${datePart}-${randPart}`;

    const newCase: CaseRequest = {
      id,
      protocol,
      ownerSubject: user.id,
      ownerEmail: user.email,
      title: payload.title,
      description: payload.description,
      caseType: payload.type || 'ANALISE_DOCUMENTAL',
      status: 'RASCUNHO',
      version: 1,
      processingRun: 0,
      rulesVersion: 'DOCUMENTAL_V1',
      submittedAt: null,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      documents: []
    };

    this.cases.unshift(newCase);
    this.addHistory(id, 'CRIACAO_RASCUNHO', user.email, `Rascunho criado com protocolo ${protocol}`);
    this.save();
    return newCase;
  }

  updateCase(id: string, title: string, description: string, version: number, user: User): CaseRequest {
    const caseReq = this.getCaseById(id, user);
    if (!caseReq) throw new Error('Caso não encontrado');
    if (caseReq.status !== 'RASCUNHO') throw new Error('Solicitação imutável');
    if (caseReq.version !== version) throw new Error('Conflito de versão');

    caseReq.title = title;
    caseReq.description = description;
    caseReq.version += 1;
    caseReq.updatedAt = new Date().toISOString();

    this.addHistory(id, 'RASCUNHO_ATUALIZADO', user.email, 'Título e descrição atualizados');
    this.save();
    return caseReq;
  }

  addDocument(caseId: string, category: DocumentCategory, fileName: string, validUntil: string | null, user: User): CaseDocument {
    const caseReq = this.getCaseById(caseId, user);
    if (!caseReq) throw new Error('Caso não encontrado');
    if (caseReq.status !== 'RASCUNHO') throw new Error('Não é possível anexar documentos após o envio');

    // Remove documento existente na mesma categoria
    caseReq.documents = caseReq.documents.filter(d => d.category !== category);

    const doc: CaseDocument = {
      id: 'doc-' + Math.random().toString(36).substring(2, 9),
      category,
      fileName,
      fileSize: 1024 * 1024 * 1.5,
      contentType: 'application/pdf',
      validUntil,
      uploadState: 'READY',
      createdAt: new Date().toISOString()
    };

    caseReq.documents.push(doc);
    caseReq.version += 1;
    caseReq.updatedAt = new Date().toISOString();

    this.addHistory(caseId, 'DOCUMENTO_ANEXADO', user.email, `Anexado arquivo ${fileName} para ${category}`);
    this.save();
    return doc;
  }

  removeDocument(caseId: string, documentId: string, user: User) {
    const caseReq = this.getCaseById(caseId, user);
    if (!caseReq) throw new Error('Caso não encontrado');
    if (caseReq.status !== 'RASCUNHO') throw new Error('Não é possível remover documentos');

    const doc = caseReq.documents.find(d => d.id === documentId);
    caseReq.documents = caseReq.documents.filter(d => d.id !== documentId);
    caseReq.version += 1;
    caseReq.updatedAt = new Date().toISOString();

    if (doc) {
      this.addHistory(caseId, 'DOCUMENTO_REMOVIDO', user.email, `Removido documento ${doc.category}`);
    }
    this.save();
  }

  submitCase(id: string, version: number, user: User): CaseRequest {
    const caseReq = this.getCaseById(id, user);
    if (!caseReq) throw new Error('Caso não encontrado');
    if (caseReq.status !== 'RASCUNHO') throw new Error('Caso não está em rascunho');
    if (caseReq.documents.length === 0) throw new Error('Envio exige pelo menos um documento anexado');

    caseReq.status = 'PROCESSANDO';
    caseReq.version += 1;
    caseReq.processingRun += 1;
    caseReq.submittedAt = new Date().toISOString();
    caseReq.updatedAt = new Date().toISOString();

    this.addHistory(id, 'SOLICITACAO_ENVIADA', user.email, `Enviado para análise (Run #${caseReq.processingRun})`);
    this.save();

    // Simula motor de regras assíncrono após 1.5s
    setTimeout(() => {
      this.evaluateCase(id);
    }, 1500);

    return caseReq;
  }

  retryCase(id: string, justification: string, user: User): CaseRequest {
    if (user.role !== 'ROLE_ADMIN') throw new Error('Apenas ADMIN pode reprocessar');
    const caseReq = this.getCaseById(id, user);
    if (!caseReq) throw new Error('Caso não encontrado');
    if (caseReq.status !== 'FALHA_TECNICA') throw new Error('Reprocessamento exclusivo para falha técnica');

    caseReq.status = 'PROCESSANDO';
    caseReq.processingRun += 1;
    caseReq.version += 1;
    caseReq.updatedAt = new Date().toISOString();

    this.addHistory(id, 'REPROCESSAMENTO_SOLICITADO', user.email, `Justificativa: ${justification}`);
    this.save();

    // Reavaliação
    setTimeout(() => {
      this.evaluateCase(id);
    }, 1500);

    return caseReq;
  }

  evaluateCase(id: string) {
    const caseReq = this.cases.find(c => c.id === id);
    if (!caseReq) return;

    const reasons: string[] = [];
    const today = new Date().toISOString().split('T')[0];

    // Simulação de falha técnica
    if (caseReq.title.includes('[SIMULAR_FALHA]') || caseReq.description.includes('[SIMULAR_FALHA]')) {
      caseReq.status = 'FALHA_TECNICA';
      caseReq.latestResult = {
        id: 'res-' + Math.random().toString(36).substring(2, 9),
        runNumber: caseReq.processingRun,
        decision: 'FALHA_TECNICA',
        reasonCodes: ['FALHA_INFRAESTRUTURA'],
        rulesVersion: caseReq.rulesVersion,
        evaluatedAt: new Date().toISOString()
      };
      this.addHistory(id, 'FALHA_TECNICA_REGISTRADA', 'SYSTEM', 'Simulação de falha de I/O em disco');
      this.addNotification(caseReq.id, 'Falha Técnica Detectada', `Instabilidade na análise do protocolo ${caseReq.protocol}`);
      this.save();
      return;
    }

    // Regra 1: IDENTIFICACAO
    const docId = caseReq.documents.find(d => d.category === 'IDENTIFICACAO');
    if (!docId) {
      reasons.push('FALTA_IDENTIFICACAO');
    } else if (docId.validUntil && docId.validUntil < today) {
      reasons.push('DOCUMENTO_VENCIDO_IDENTIFICACAO');
    }

    // Regra 2: COMPROVANTE_ENDERECO
    const docEnd = caseReq.documents.find(d => d.category === 'COMPROVANTE_ENDERECO');
    if (!docEnd) {
      reasons.push('FALTA_COMPROVANTE_ENDERECO');
    } else if (docEnd.validUntil && docEnd.validUntil < today) {
      reasons.push('DOCUMENTO_VENCIDO_COMPROVANTE_ENDERECO');
    }

    const decision = reasons.length === 0 ? 'APROVADA' : 'REJEITADA';
    caseReq.status = decision;
    caseReq.latestResult = {
      id: 'res-' + Math.random().toString(36).substring(2, 9),
      runNumber: caseReq.processingRun,
      decision,
      reasonCodes: reasons,
      rulesVersion: caseReq.rulesVersion,
      evaluatedAt: new Date().toISOString()
    };

    if (decision === 'APROVADA') {
      this.addHistory(id, 'ANALISE_APROVADA', 'SYSTEM', 'Critérios cumpridos integralmente na regra DOCUMENTAL_V1');
      this.addNotification(caseReq.id, `Solicitação ${caseReq.protocol} Aprovada`, 'Documentos conferidos e aceitos.');
    } else {
      this.addHistory(id, 'ANALISE_REJEITADA', 'SYSTEM', `Motivos: ${reasons.join(', ')}`);
      this.addNotification(caseReq.id, `Solicitação ${caseReq.protocol} Rejeitada`, `Pendências: ${reasons.join(', ')}`);
    }

    this.save();
  }

  addHistory(caseId: string, eventType: string, actorSubject: string, details: string) {
    if (!this.histories[caseId]) {
      this.histories[caseId] = [];
    }
    this.histories[caseId].unshift({
      id: 'hist-' + Math.random().toString(36).substring(2, 9),
      caseId,
      eventType,
      actorSubject,
      details,
      occurredAt: new Date().toISOString()
    });
  }

  addNotification(caseId: string, title: string, message: string) {
    this.notifications.unshift({
      id: 'notif-' + Math.random().toString(36).substring(2, 9),
      caseId,
      title,
      message,
      readAt: null,
      createdAt: new Date().toISOString()
    });
  }

  getHistories(caseId: string): CaseHistory[] {
    return this.histories[caseId] || [];
  }

  getNotifications(): NotificationItem[] {
    return [...this.notifications];
  }

  markNotificationRead(id: string) {
    const notif = this.notifications.find(n => n.id === id);
    if (notif) {
      notif.readAt = new Date().toISOString();
      this.save();
    }
  }
}

export const localStore = new StorageService();

// API Client that checks backend availability or falls back to localStore
export class ApiService {
  private static backendAvailable: boolean | null = null;

  static async checkBackend(): Promise<boolean> {
    try {
      const res = await fetch('/bff/v1/csrf', { signal: AbortSignal.timeout(1000) });
      this.backendAvailable = res.ok;
      return res.ok;
    } catch {
      this.backendAvailable = false;
      return false;
    }
  }

  static isBackendOnline(): boolean {
    return this.backendAvailable ?? false;
  }

  static async getCases(user: User): Promise<CaseRequest[]> {
    if (this.backendAvailable) {
      try {
        const res = await fetch('/bff/v1/cases', {
          headers: { 'X-User-Email': user.email }
        });
        if (res.ok) return await res.json();
      } catch (err) {
        console.warn('Backend indisponível, usando fallback em memória', err);
      }
    }
    return localStore.getCases(user);
  }

  static async getCaseById(id: string, user: User): Promise<CaseRequest | null> {
    if (this.backendAvailable) {
      try {
        const res = await fetch(`/bff/v1/cases/${id}`, {
          headers: { 'X-User-Email': user.email }
        });
        if (res.ok) return await res.json();
      } catch (err) {
        console.warn('Backend indisponível, usando fallback em memória', err);
      }
    }
    return localStore.getCaseById(id, user);
  }

  static async createCase(payload: CreateCasePayload, user: User): Promise<CaseRequest> {
    if (this.backendAvailable) {
      try {
        const res = await fetch('/bff/v1/cases', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'X-User-Email': user.email
          },
          body: JSON.stringify(payload)
        });
        if (res.ok) return await res.json();
      } catch (err) {
        console.warn('Backend indisponível, usando fallback em memória', err);
      }
    }
    return localStore.createCase(payload, user);
  }

  static async uploadDocument(
    caseId: string,
    category: DocumentCategory,
    validUntil: string | null,
    file: File,
    user: User
  ): Promise<CaseDocument> {
    if (this.backendAvailable) {
      try {
        const formData = new FormData();
        formData.append('category', category);
        if (validUntil) formData.append('validUntil', validUntil);
        formData.append('file', file);

        const res = await fetch(`/bff/v1/cases/${caseId}/documents`, {
          method: 'POST',
          headers: { 'X-User-Email': user.email },
          body: formData
        });
        if (res.ok) return await res.json();
      } catch (err) {
        console.warn('Backend indisponível, usando fallback em memória', err);
      }
    }
    return localStore.addDocument(caseId, category, file.name, validUntil, user);
  }

  static async deleteDocument(caseId: string, documentId: string, user: User): Promise<void> {
    if (this.backendAvailable) {
      try {
        const res = await fetch(`/bff/v1/cases/${caseId}/documents/${documentId}`, {
          method: 'DELETE',
          headers: { 'X-User-Email': user.email }
        });
        if (res.ok) return;
      } catch (err) {
        console.warn('Backend indisponível, usando fallback em memória', err);
      }
    }
    localStore.removeDocument(caseId, documentId, user);
  }

  static async submitCase(id: string, version: number, user: User): Promise<CaseRequest> {
    if (this.backendAvailable) {
      try {
        const res = await fetch(`/bff/v1/cases/${id}/submit`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'X-User-Email': user.email,
            'Idempotency-Key': `sub-${id}-${version}`
          },
          body: JSON.stringify({ version })
        });
        if (res.ok) return await res.json();
      } catch (err) {
        console.warn('Backend indisponível, usando fallback em memória', err);
      }
    }
    return localStore.submitCase(id, version, user);
  }

  static async retryCase(id: string, justification: string, user: User): Promise<CaseRequest> {
    if (this.backendAvailable) {
      try {
        const res = await fetch(`/bff/v1/cases/${id}/retry`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'X-User-Email': user.email,
            'Idempotency-Key': `retry-${id}-${Date.now()}`
          },
          body: JSON.stringify({ justification })
        });
        if (res.ok) return await res.json();
      } catch (err) {
        console.warn('Backend indisponível, usando fallback em memória', err);
      }
    }
    return localStore.retryCase(id, justification, user);
  }

  static async getHistory(caseId: string, user: User): Promise<CaseHistory[]> {
    if (this.backendAvailable) {
      try {
        const res = await fetch(`/bff/v1/cases/${caseId}/history`, {
          headers: { 'X-User-Email': user.email }
        });
        if (res.ok) return await res.json();
      } catch (err) {
        console.warn('Backend indisponível, usando fallback em memória', err);
      }
    }
    return localStore.getHistories(caseId);
  }

  static async getNotifications(user: User): Promise<NotificationItem[]> {
    if (this.backendAvailable) {
      try {
        const res = await fetch('/bff/v1/notifications', {
          headers: { 'X-User-Email': user.email }
        });
        if (res.ok) return await res.json();
      } catch (err) {
        console.warn('Backend indisponível, usando fallback em memória', err);
      }
    }
    return localStore.getNotifications();
  }

  static async markNotificationRead(id: string, user: User): Promise<void> {
    if (this.backendAvailable) {
      try {
        await fetch(`/bff/v1/notifications/${id}`, {
          method: 'PATCH',
          headers: { 'X-User-Email': user.email }
        });
        return;
      } catch (err) {
        console.warn('Backend indisponível, usando fallback em memória', err);
      }
    }
    localStore.markNotificationRead(id);
  }
}
