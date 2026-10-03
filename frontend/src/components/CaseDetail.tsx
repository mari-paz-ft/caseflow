import React from 'react';
import { User } from '../types';
import { StatusBadge } from './StatusBadge';
import { useCaseDetail } from '../hooks/useCaseDetail';
import { DocumentUploadForm } from './CaseDetail/DocumentUploadForm';
import { CaseHistoryTimeline } from './CaseDetail/CaseHistoryTimeline';
import { RetryModal } from './CaseDetail/RetryModal';
import {
  ArrowLeft,
  FileText,
  Trash2,
  Send,
  RotateCcw,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Layers,
  FileCheck,
  Download,
  AlertCircle
} from 'lucide-react';

interface Props {
  caseId: string;
  currentUser: User;
  onBack: () => void;
  onRefreshList: () => void;
}

export const CaseDetail: React.FC<Props> = ({
  caseId,
  currentUser,
  onBack,
  onRefreshList
}) => {
  const {
    caseData,
    histories,
    loading,
    uploadCategory,
    setUploadCategory,
    validUntil,
    setValidUntil,
    selectedFile,
    setSelectedFile,
    isUploading,
    isRetryModalOpen,
    setIsRetryModalOpen,
    retryJustification,
    setRetryJustification,
    isRetrying,
    isSubmitting,
    isOwner,
    isAdmin,
    isDraft,
    isTechnicalFailure,
    hasIdDoc,
    hasAddressDoc,
    handleUpload,
    handleDeleteDocument,
    handleSubmitCase,
    handleRetrySubmit,
  } = useCaseDetail(caseId, currentUser, onRefreshList);

  if (loading || !caseData) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-sm font-medium text-slate-500 animate-pulse flex items-center space-x-2">
          <Layers className="w-5 h-5 animate-spin text-brand-600" />
          <span>Carregando solicitação...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-in fade-in duration-150">
      {/* Top action bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <button
          onClick={onBack}
          className="inline-flex items-center space-x-1.5 text-sm font-medium text-slate-600 hover:text-slate-900 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Voltar para solicitações</span>
        </button>

        <div className="flex items-center space-x-3">
          {/* Ação: Enviar para Análise */}
          {isOwner && isDraft && (
            <button
              onClick={handleSubmitCase}
              disabled={isSubmitting || caseData.documents.length === 0}
              className="inline-flex items-center space-x-2 px-4 py-2 bg-brand-600 hover:bg-brand-700 disabled:opacity-50 text-white rounded-xl text-sm font-semibold shadow-sm transition-colors"
            >
              <Send className="w-4 h-4" />
              <span>{isSubmitting ? 'Enviando...' : 'Enviar para Análise'}</span>
            </button>
          )}

          {/* Ação: Reprocessar Falha Técnica (Admin) */}
          {isAdmin && isTechnicalFailure && (
            <button
              onClick={() => setIsRetryModalOpen(true)}
              className="inline-flex items-center space-x-2 px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white rounded-xl text-sm font-semibold shadow-sm transition-colors"
            >
              <RotateCcw className="w-4 h-4" />
              <span>Reprocessar Falha Técnica</span>
            </button>
          )}
        </div>
      </div>

      {/* Main details card */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6 space-y-6">
        {/* Header */}
        <div className="border-b border-slate-100 pb-5">
          <div className="flex flex-wrap items-center justify-between gap-3 mb-2">
            <div className="flex items-center space-x-3">
              <span className="font-mono text-sm font-bold bg-slate-100 text-slate-800 px-3 py-1 rounded-lg">
                {caseData.protocol}
              </span>
              <StatusBadge status={caseData.status} size="lg" />
            </div>
            <div className="text-xs text-slate-400 flex items-center space-x-4">
              <span>Versão: v{caseData.version}</span>
              <span>Execuções: #{caseData.processingRun}</span>
              <span>Regras: {caseData.rulesVersion}</span>
            </div>
          </div>
          <h1 className="text-xl font-bold text-slate-900">{caseData.title}</h1>
          <p className="text-sm text-slate-600 mt-1 whitespace-pre-wrap">{caseData.description}</p>

          <div className="mt-4 flex flex-wrap gap-4 text-xs text-slate-500 pt-3 border-t border-slate-50">
            <div>
              <span className="font-semibold text-slate-700">Autor:</span> {caseData.ownerEmail}
            </div>
            <div>
              <span className="font-semibold text-slate-700">Criado em:</span>{' '}
              {new Date(caseData.createdAt).toLocaleString()}
            </div>
            {caseData.submittedAt && (
              <div>
                <span className="font-semibold text-slate-700">Enviado em:</span>{' '}
                {new Date(caseData.submittedAt).toLocaleString()}
              </div>
            )}
          </div>
        </div>

        {/* Processing Result Banner */}
        {caseData.latestResult && (
          <div
            className={`p-5 rounded-xl border ${
              caseData.latestResult.decision === 'APROVADA'
                ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
                : caseData.latestResult.decision === 'REJEITADA'
                ? 'bg-rose-50 border-rose-200 text-rose-900'
                : 'bg-amber-50 border-amber-200 text-amber-900'
            }`}
          >
            <div className="flex items-start space-x-3">
              {caseData.latestResult.decision === 'APROVADA' ? (
                <CheckCircle2 className="w-6 h-6 text-emerald-600 shrink-0 mt-0.5" />
              ) : caseData.latestResult.decision === 'REJEITADA' ? (
                <XCircle className="w-6 h-6 text-rose-600 shrink-0 mt-0.5" />
              ) : (
                <AlertTriangle className="w-6 h-6 text-amber-600 shrink-0 mt-0.5" />
              )}
              <div className="space-y-2 flex-1">
                <div className="flex items-center justify-between">
                  <h3 className="font-bold text-base">
                    Resultado da Conferência: {caseData.latestResult.decision}
                  </h3>
                  <span className="text-xs opacity-75">
                    Avaliado em: {new Date(caseData.latestResult.evaluatedAt).toLocaleTimeString()} (Run #{caseData.latestResult.runNumber})
                  </span>
                </div>

                {caseData.latestResult.decision === 'APROVADA' && (
                  <p className="text-xs text-emerald-800">
                    Todos os critérios de obrigatoriedade e vigência documental foram atendidos com sucesso conforme as diretrizes da regra {caseData.rulesVersion}.
                  </p>
                )}

                {caseData.latestResult.decision === 'REJEITADA' && (
                  <div>
                    <p className="text-xs text-rose-800 font-medium mb-1.5">
                      A solicitação foi recusada pelos seguintes motivos determinísticos:
                    </p>
                    <div className="flex flex-wrap gap-2">
                      {caseData.latestResult.reasonCodes.map(code => {
                        const descriptions: Record<string, string> = {
                          FALTA_IDENTIFICACAO: 'Documento de Identificação ausente',
                          FALTA_COMPROVANTE_ENDERECO: 'Comprovante de Residência ausente',
                          DOCUMENTO_VENCIDO_IDENTIFICACAO: 'Identificação com data de validade vencida',
                          DOCUMENTO_VENCIDO_COMPROVANTE_ENDERECO: 'Comprovante de residência com validade expirada',
                          DOCUMENTO_VENCIDO_COMPLEMENTAR: 'Documento complementar vencido'
                        };
                        return (
                          <span
                            key={code}
                            className="bg-rose-100 text-rose-800 border border-rose-300 text-xs px-2.5 py-1 rounded-md font-semibold"
                          >
                            {descriptions[code] || code}
                          </span>
                        );
                      })}
                    </div>
                    <p className="text-[11px] text-rose-700 mt-2 italic">
                      * Nota de arquitetura: solicitações rejeitadas tornam-se imutáveis e exigem a criação de um novo rascunho para reenvio.
                    </p>
                  </div>
                )}

                {caseData.latestResult.decision === 'FALHA_TECNICA' && (
                  <div className="text-xs text-amber-800 space-y-1">
                    <p className="font-semibold">
                      Ocorreu uma falha técnica de infraestrutura durante a análise.
                    </p>
                    <p>
                      O arquivo ou recurso de disco ficou temporariamente indisponível. Somente um Administrador pode disparar um novo processamento com justificativa registrada.
                    </p>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Documents Section */}
        <div className="space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-100 pb-3">
            <div>
              <h2 className="text-base font-bold text-slate-900 flex items-center space-x-2">
                <FileCheck className="w-5 h-5 text-brand-600" />
                <span>Documentos Comprobatórios</span>
                <span className="text-xs bg-slate-100 text-slate-600 px-2 py-0.5 rounded-full font-semibold">
                  {caseData.documents.length}/3
                </span>
              </h2>
              <p className="text-xs text-slate-500">
                Máximo de 3 arquivos (um por categoria: Identificação, Comprovante de Residência e Complementar).
              </p>
            </div>

            {/* Checklist badges */}
            <div className="flex items-center space-x-2 text-xs">
              <span
                className={`px-2.5 py-1 rounded-lg border font-medium flex items-center space-x-1 ${
                  hasIdDoc
                    ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                    : 'bg-slate-100 text-slate-500 border-slate-200'
                }`}
              >
                <span>Identificação:</span>
                <b>{hasIdDoc ? 'Anexado' : 'Pendente'}</b>
              </span>
              <span
                className={`px-2.5 py-1 rounded-lg border font-medium flex items-center space-x-1 ${
                  hasAddressDoc
                    ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                    : 'bg-slate-100 text-slate-500 border-slate-200'
                }`}
              >
                <span>Endereço:</span>
                <b>{hasAddressDoc ? 'Anexado' : 'Pendente'}</b>
              </span>
            </div>
          </div>

          {/* Documents list */}
          {caseData.documents.length === 0 ? (
            <div className="border border-dashed border-slate-300 rounded-xl p-8 text-center text-slate-500 text-xs">
              Nenhum documento anexado ainda a esta solicitação.
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              {caseData.documents.map(doc => {
                const categoryLabels: Record<string, { label: string; color: string }> = {
                  IDENTIFICACAO: { label: 'Identificação (Obrigatório)', color: 'bg-blue-100 text-blue-800' },
                  COMPROVANTE_ENDERECO: { label: 'Comprovante de Endereço (Obrigatório)', color: 'bg-purple-100 text-purple-800' },
                  COMPLEMENTAR: { label: 'Documento Complementar (Opcional)', color: 'bg-slate-100 text-slate-800' }
                };
                const catInfo = categoryLabels[doc.category] || { label: doc.category, color: 'bg-gray-100' };

                const isExpired =
                  doc.validUntil &&
                  new Date(doc.validUntil) < new Date(caseData.submittedAt || new Date().toISOString());

                return (
                  <div
                    key={doc.id}
                    className="p-4 rounded-xl border border-slate-200 bg-slate-50/50 hover:bg-slate-50 transition-colors flex flex-col justify-between"
                  >
                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <span className={`text-[11px] font-bold px-2 py-0.5 rounded ${catInfo.color}`}>
                          {catInfo.label}
                        </span>
                        {isExpired && (
                          <span className="text-[10px] font-bold bg-rose-100 text-rose-700 px-1.5 py-0.5 rounded flex items-center space-x-0.5">
                            <AlertCircle className="w-3 h-3" />
                            <span>Vencido</span>
                          </span>
                        )}
                      </div>

                      <div className="flex items-center space-x-2 text-slate-900 font-semibold text-sm">
                        <FileText className="w-4 h-4 text-brand-600 shrink-0" />
                        <span className="truncate" title={doc.fileName}>
                          {doc.fileName}
                        </span>
                      </div>

                      <div className="mt-2 text-[11px] text-slate-500 space-y-0.5">
                        <p>Tamanho: {(doc.fileSize / 1024 / 1024).toFixed(2)} MB</p>
                        <p>
                          Validade declarada:{' '}
                          <span className={isExpired ? 'text-rose-600 font-bold' : 'text-slate-700'}>
                            {doc.validUntil ? new Date(doc.validUntil).toLocaleDateString() : 'Não informada'}
                          </span>
                        </p>
                      </div>
                    </div>

                    <div className="mt-4 pt-3 border-t border-slate-200/60 flex items-center justify-between">
                      <button
                        onClick={() => alert(`Download simulado do arquivo ${doc.fileName}`)}
                        className="text-xs font-medium text-brand-600 hover:text-brand-800 flex items-center space-x-1"
                      >
                        <Download className="w-3.5 h-3.5" />
                        <span>Baixar arquivo</span>
                      </button>

                      {isOwner && isDraft && (
                        <button
                          onClick={() => handleDeleteDocument(doc.id)}
                          className="text-xs text-rose-600 hover:text-rose-800 p-1 rounded hover:bg-rose-50"
                          title="Remover documento"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* Upload Form (only in Rascunho for owner) */}
          {isOwner && isDraft && caseData.documents.length < 3 && (
            <DocumentUploadForm
              category={uploadCategory}
              onCategoryChange={setUploadCategory}
              validUntil={validUntil}
              onValidUntilChange={setValidUntil}
              onFileSelected={setSelectedFile}
              hasSelectedFile={selectedFile !== null}
              isUploading={isUploading}
              onSubmit={handleUpload}
            />
          )}
        </div>

        <CaseHistoryTimeline histories={histories} />
      </div>

      <RetryModal
        isOpen={isRetryModalOpen}
        isRetrying={isRetrying}
        justification={retryJustification}
        onJustificationChange={setRetryJustification}
        onClose={() => setIsRetryModalOpen(false)}
        onSubmit={handleRetrySubmit}
      />
    </div>
  );
};
