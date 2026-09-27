import {
  CaseDocument,
  CaseHistory,
  CaseRequest,
  CreateCasePayload,
  DocumentCategory,
  NotificationItem,
  UpdateCasePayload,
  User
} from '../types';

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresAt: string;
  user: User;
}

export class ApiService {
  private static accessToken: string | null = null;

  static setAccessToken(token: string | null) {
    this.accessToken = token;
  }

  static async login(username: string, password: string): Promise<LoginResponse> {
    return this.requestJson<LoginResponse>('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    }, false);
  }

  static async getCases(): Promise<CaseRequest[]> {
    return this.requestJson('/bff/v1/cases');
  }

  static async getCaseById(id: string): Promise<CaseRequest> {
    return this.requestJson(`/bff/v1/cases/${id}`);
  }

  static async createCase(payload: CreateCasePayload): Promise<CaseRequest> {
    return this.requestJson('/bff/v1/cases', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  }

  static async updateCase(id: string, payload: UpdateCasePayload): Promise<CaseRequest> {
    return this.requestJson(`/bff/v1/cases/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    });
  }

  static async uploadDocument(
    caseId: string,
    category: DocumentCategory,
    validUntil: string | null,
    file: File
  ): Promise<CaseDocument> {
    const formData = new FormData();
    formData.append('category', category);
    if (validUntil) formData.append('validUntil', validUntil);
    formData.append('file', file);
    return this.requestJson(`/bff/v1/cases/${caseId}/documents`, {
      method: 'POST',
      body: formData
    });
  }

  static async downloadDocument(caseId: string, documentId: string): Promise<Blob> {
    const response = await this.send(`/bff/v1/cases/${caseId}/documents/${documentId}/content`);
    return response.blob();
  }

  static async deleteDocument(caseId: string, documentId: string): Promise<void> {
    await this.send(`/bff/v1/cases/${caseId}/documents/${documentId}`, { method: 'DELETE' });
  }

  static async submitCase(id: string, version: number): Promise<CaseRequest> {
    return this.requestJson(`/bff/v1/cases/${id}/submit`, {
      method: 'POST',
      headers: { 'Idempotency-Key': `submit-${id}-${version}` },
      body: JSON.stringify({ version })
    });
  }

  static async retryCase(id: string, justification: string, idempotencyKey: string): Promise<CaseRequest> {
    return this.requestJson(`/bff/v1/cases/${id}/retry`, {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKey },
      body: JSON.stringify({ justification })
    });
  }

  static async getHistory(caseId: string): Promise<CaseHistory[]> {
    return this.requestJson(`/bff/v1/cases/${caseId}/history`);
  }

  static async getNotifications(): Promise<NotificationItem[]> {
    return this.requestJson('/bff/v1/notifications');
  }

  static async markNotificationRead(id: string): Promise<void> {
    await this.requestJson(`/bff/v1/notifications/${id}`, { method: 'PATCH' });
  }

  private static async requestJson<T>(path: string, init: RequestInit = {}, authenticated = true): Promise<T> {
    const response = await this.send(path, init, authenticated);
    if (response.status === 204) return undefined as T;
    return response.json() as Promise<T>;
  }

  private static async send(path: string, init: RequestInit = {}, authenticated = true): Promise<Response> {
    const headers = new Headers(init.headers);
    if (authenticated) {
      if (!this.accessToken) throw new Error('Autenticação necessária. Entre novamente.');
      headers.set('Authorization', `Bearer ${this.accessToken}`);
    }
    if (typeof init.body === 'string' && !headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json');
    }

    const response = await fetch(path, { ...init, headers });
    if (!response.ok) {
      const body = await response.clone().json().catch(() => null);
      if (response.status === 401 && authenticated) this.accessToken = null;
      throw new Error(body?.message ?? body?.detail ?? `A solicitação falhou (HTTP ${response.status}).`);
    }
    return response;
  }
}
