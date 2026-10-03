import { CaseRequest, CaseDocument, CaseHistory, NotificationItem, User, CreateCasePayload, DocumentCategory } from '../types';
import { INITIAL_CASES, INITIAL_HISTORIES, INITIAL_NOTIFICATIONS } from './fixtures';

class StorageService {
  private cases: CaseRequest[];
  private histories: Record<string, CaseHistory[]>;
  private notifications: NotificationItem[];

  constructor() {
    const storage = typeof localStorage === 'undefined' ? null : localStorage;
    const storedCases = storage?.getItem('caseflow_cases');
    const storedHistories = storage?.getItem('caseflow_histories');
    const storedNotifs = storage?.getItem('caseflow_notifs');

    this.cases = storedCases ? JSON.parse(storedCases) : INITIAL_CASES;
    this.histories = storedHistories ? JSON.parse(storedHistories) : INITIAL_HISTORIES;
    this.notifications = storedNotifs ? JSON.parse(storedNotifs) : INITIAL_NOTIFICATIONS;
  }

  save() {
    if (typeof localStorage === 'undefined') return;
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
