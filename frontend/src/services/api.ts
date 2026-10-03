import {
  CaseDocument,
  CaseHistory,
  CaseRequest,
  CreateCasePayload,
  DocumentCategory,
  NotificationItem,
  User,
} from '../types';
import { withFallback, requestJson } from './httpClient';
import { localStore } from './mockAdapter';
import { USERS } from './fixtures';

export { USERS };

export class ApiService {
  private static backendAvailable: boolean | null = null;

  static async checkBackend(): Promise<boolean> {
    try {
      const response = await fetch('/bff/v1/csrf', { signal: AbortSignal.timeout(1000) });
      this.backendAvailable = response.ok;
    } catch {
      this.backendAvailable = false;
    }
    return this.backendAvailable;
  }

  static isBackendOnline(): boolean {
    return this.backendAvailable ?? false;
  }

  private static runWithFallback<T>(backend: () => Promise<T>, fallback: () => T | Promise<T>): Promise<T> {
    return withFallback(this.backendAvailable === true, backend, fallback);
  }

  static getCases(user: User): Promise<CaseRequest[]> {
    return this.runWithFallback(
      () => requestJson('/bff/v1/cases', { headers: { 'X-User-Email': user.email } }),
      () => localStore.getCases(user),
    );
  }

  static getCaseById(id: string, user: User): Promise<CaseRequest | null> {
    return this.runWithFallback(
      () => requestJson(`/bff/v1/cases/${id}`, { headers: { 'X-User-Email': user.email } }),
      () => localStore.getCaseById(id, user),
    );
  }

  static createCase(payload: CreateCasePayload, user: User): Promise<CaseRequest> {
    return this.runWithFallback(
      () => requestJson('/bff/v1/cases', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-User-Email': user.email },
        body: JSON.stringify(payload),
      }),
      () => localStore.createCase(payload, user),
    );
  }

  static uploadDocument(
    caseId: string,
    category: DocumentCategory,
    validUntil: string | null,
    file: File,
    user: User,
  ): Promise<CaseDocument> {
    return this.runWithFallback(async () => {
      const formData = new FormData();
      formData.append('category', category);
      if (validUntil) formData.append('validUntil', validUntil);
      formData.append('file', file);
      return requestJson(`/bff/v1/cases/${caseId}/documents`, {
        method: 'POST',
        headers: { 'X-User-Email': user.email },
        body: formData,
      });
    }, () => localStore.addDocument(caseId, category, file.name, validUntil, user));
  }

  static deleteDocument(caseId: string, documentId: string, user: User): Promise<void> {
    return this.runWithFallback(
      () => requestJson(`/bff/v1/cases/${caseId}/documents/${documentId}`, {
        method: 'DELETE',
        headers: { 'X-User-Email': user.email },
      }),
      () => localStore.removeDocument(caseId, documentId, user),
    );
  }

  static submitCase(id: string, version: number, user: User): Promise<CaseRequest> {
    return this.runWithFallback(
      () => requestJson(`/bff/v1/cases/${id}/submit`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-User-Email': user.email,
          'Idempotency-Key': `sub-${id}-${version}`,
        },
        body: JSON.stringify({ version }),
      }),
      () => localStore.submitCase(id, version, user),
    );
  }

  static retryCase(id: string, justification: string, user: User): Promise<CaseRequest> {
    return this.runWithFallback(
      () => requestJson(`/bff/v1/cases/${id}/retry`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-User-Email': user.email,
          'Idempotency-Key': `retry-${id}-${Date.now()}`,
        },
        body: JSON.stringify({ justification }),
      }),
      () => localStore.retryCase(id, justification, user),
    );
  }

  static getHistory(caseId: string, user: User): Promise<CaseHistory[]> {
    return this.runWithFallback(
      () => requestJson(`/bff/v1/cases/${caseId}/history`, { headers: { 'X-User-Email': user.email } }),
      () => localStore.getHistories(caseId),
    );
  }

  static getNotifications(user: User): Promise<NotificationItem[]> {
    return this.runWithFallback(
      () => requestJson('/bff/v1/notifications', { headers: { 'X-User-Email': user.email } }),
      () => localStore.getNotifications(),
    );
  }

  static markNotificationRead(id: string, user: User): Promise<void> {
    return this.runWithFallback(
      () => requestJson(`/bff/v1/notifications/${id}`, {
        method: 'PATCH',
        headers: { 'X-User-Email': user.email },
      }),
      () => localStore.markNotificationRead(id),
    );
  }
}
