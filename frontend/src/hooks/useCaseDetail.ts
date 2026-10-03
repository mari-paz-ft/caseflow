import { useCallback, useEffect, useState, FormEvent } from 'react'
import { ApiService } from '../services/api'
import { CaseHistory, CaseRequest, DocumentCategory, User } from '../types'

function messageFrom(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback
}

export function useCaseDetail(caseId: string, currentUser: User, onRefreshList: () => void) {
  const [caseData, setCaseData] = useState<CaseRequest | null>(null)
  const [histories, setHistories] = useState<CaseHistory[]>([])
  const [loading, setLoading] = useState(true)
  const [uploadCategory, setUploadCategory] = useState<DocumentCategory>('IDENTIFICACAO')
  const [validUntil, setValidUntil] = useState('')
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [isUploading, setIsUploading] = useState(false)
  const [isRetryModalOpen, setIsRetryModalOpen] = useState(false)
  const [retryJustification, setRetryJustification] = useState('')
  const [isRetrying, setIsRetrying] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const loadCase = useCallback(async () => {
    try {
      const data = await ApiService.getCaseById(caseId, currentUser)
      setCaseData(data)
      if (data) setHistories(await ApiService.getHistory(data.id, currentUser))
    } catch (error) {
      console.error('Erro ao carregar detalhes:', error)
    } finally {
      setLoading(false)
    }
  }, [caseId, currentUser])

  useEffect(() => {
    void loadCase()
  }, [loadCase])

  useEffect(() => {
    if (caseData?.status !== 'PROCESSANDO') return undefined
    const interval = window.setInterval(() => void loadCase(), 2000)
    return () => window.clearInterval(interval)
  }, [caseData?.status, loadCase])

  const handleUpload = useCallback(async (event: FormEvent) => {
    event.preventDefault()
    if (!selectedFile || !caseData) return

    try {
      setIsUploading(true)
      await ApiService.uploadDocument(caseData.id, uploadCategory, validUntil || null, selectedFile, currentUser)
      setSelectedFile(null)
      setValidUntil('')
      await loadCase()
      onRefreshList()
    } catch (error) {
      alert(messageFrom(error, 'Falha no upload do documento'))
    } finally {
      setIsUploading(false)
    }
  }, [caseData, currentUser, loadCase, onRefreshList, selectedFile, uploadCategory, validUntil])

  const handleDeleteDocument = useCallback(async (documentId: string) => {
    if (!caseData || !confirm('Deseja realmente remover este documento?')) return
    try {
      await ApiService.deleteDocument(caseData.id, documentId, currentUser)
      await loadCase()
      onRefreshList()
    } catch (error) {
      alert(messageFrom(error, 'Erro ao remover anexo'))
    }
  }, [caseData, currentUser, loadCase, onRefreshList])

  const handleSubmitCase = useCallback(async () => {
    if (!caseData) return
    if (caseData.documents.length === 0) {
      alert('Envio bloqueado: adicione pelo menos um documento.')
      return
    }

    const hasIdDoc = caseData.documents.some(document => document.category === 'IDENTIFICACAO')
    const hasAddressDoc = caseData.documents.some(document => document.category === 'COMPROVANTE_ENDERECO')
    if (!hasIdDoc || !hasAddressDoc) {
      const confirmMessage =
        'Atenção: sua solicitação ainda não possui todos os documentos obrigatórios (Identificação e Comprovante de Residência). Deseja enviar mesmo assim? (A análise automática resultará em rejeição).'
      if (!confirm(confirmMessage)) return
    }

    try {
      setIsSubmitting(true)
      await ApiService.submitCase(caseData.id, caseData.version, currentUser)
      await loadCase()
      onRefreshList()
    } catch (error) {
      alert(messageFrom(error, 'Erro ao enviar solicitação'))
    } finally {
      setIsSubmitting(false)
    }
  }, [caseData, currentUser, loadCase, onRefreshList])

  const handleRetrySubmit = useCallback(async (event: FormEvent) => {
    event.preventDefault()
    if (!caseData) return
    const justification = retryJustification.trim()
    if (justification.length < 10 || justification.length > 500) {
      alert('A justificativa deve ter entre 10 e 500 caracteres.')
      return
    }

    try {
      setIsRetrying(true)
      await ApiService.retryCase(caseData.id, justification, currentUser)
      setIsRetryModalOpen(false)
      setRetryJustification('')
      await loadCase()
      onRefreshList()
    } catch (error) {
      alert(messageFrom(error, 'Erro ao reprocessar'))
    } finally {
      setIsRetrying(false)
    }
  }, [caseData, currentUser, loadCase, onRefreshList, retryJustification])

  const isOwner = caseData?.ownerSubject === currentUser.id
  const isAdmin = currentUser.role === 'ROLE_ADMIN'

  return {
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
    isDraft: caseData?.status === 'RASCUNHO',
    isTechnicalFailure: caseData?.status === 'FALHA_TECNICA',
    hasIdDoc: caseData?.documents.some(document => document.category === 'IDENTIFICACAO') ?? false,
    hasAddressDoc: caseData?.documents.some(document => document.category === 'COMPROVANTE_ENDERECO') ?? false,
    loadCase,
    handleUpload,
    handleDeleteDocument,
    handleSubmitCase,
    handleRetrySubmit,
  }
}
