export type CaseStatus = 
  | 'RASCUNHO' 
  | 'ENVIADA' 
  | 'PROCESSANDO' 
  | 'APROVADA' 
  | 'REJEITADA' 
  | 'FALHA_TECNICA';

export type DocumentCategory = 
  | 'IDENTIFICACAO' 
  | 'COMPROVANTE_ENDERECO' 
  | 'COMPLEMENTAR';

export type UploadState = 'PENDING' | 'READY' | 'FAILED';

export type RoleName = 'ROLE_USER' | 'ROLE_ADMIN';

export interface User {
  id: string;
  email: string;
  fullName: string;
  role: RoleName;
}

export interface CaseDocument {
  id: string;
  category: DocumentCategory;
  fileName: string;
  fileSize: number;
  contentType: string;
  validUntil: string | null;
  uploadState: UploadState;
  createdAt: string;
}

export interface ProcessingResult {
  id: string;
  runNumber: number;
  decision: 'APROVADA' | 'REJEITADA' | 'FALHA_TECNICA';
  reasonCodes: string[];
  rulesVersion: string;
  evaluatedAt: string;
}

export interface CaseRequest {
  id: string;
  protocol: string;
  ownerSubject: string;
  ownerEmail: string;
  title: string;
  description: string;
  caseType: string;
  status: CaseStatus;
  version: number;
  processingRun: number;
  rulesVersion: string;
  submittedAt: string | null;
  createdAt: string;
  updatedAt: string;
  documents: CaseDocument[];
  latestResult?: ProcessingResult | null;
}

export interface CaseHistory {
  id: string;
  caseId: string;
  eventType: string;
  actorSubject: string;
  details: string | null;
  occurredAt: string;
}

export interface NotificationItem {
  id: string;
  caseId: string;
  title: string;
  message: string;
  readAt: string | null;
  createdAt: string;
}

export interface CreateCasePayload {
  title: string;
  description: string;
  type?: string;
}

export interface UpdateCasePayload {
  title: string;
  description: string;
  version: number;
}
