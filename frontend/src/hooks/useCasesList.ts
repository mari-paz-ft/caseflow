import { useCallback, useEffect, useMemo, useState } from 'react'
import { ApiService } from '../services/api'
import { CaseRequest, CaseStatus, CreateCasePayload, NotificationItem, User } from '../types'

export type StatusFilter = CaseStatus | 'TODOS'

export interface CaseStats {
  total: number
  rascunho: number
  aprovadas: number
  rejeitadas: number
  falhas: number
}

const CASE_STATUSES: readonly CaseStatus[] = [
  'RASCUNHO',
  'ENVIADA',
  'PROCESSANDO',
  'APROVADA',
  'REJEITADA',
  'FALHA_TECNICA',
]

export function parseStatusFilter(value: string): StatusFilter | null {
  if (value === 'TODOS') return value
  return CASE_STATUSES.find(status => status === value) ?? null
}

export function filterCases(cases: CaseRequest[], statusFilter: StatusFilter, searchQuery: string): CaseRequest[] {
  const normalizedQuery = searchQuery.toLowerCase()
  return cases.filter(caseItem => {
    const matchesStatus = statusFilter === 'TODOS' || caseItem.status === statusFilter
    const matchesSearch =
      caseItem.title.toLowerCase().includes(normalizedQuery) ||
      caseItem.protocol.toLowerCase().includes(normalizedQuery) ||
      caseItem.description.toLowerCase().includes(normalizedQuery)
    return matchesStatus && matchesSearch
  })
}

export function computeStats(cases: CaseRequest[]): CaseStats {
  return {
    total: cases.length,
    rascunho: cases.filter(caseItem => caseItem.status === 'RASCUNHO').length,
    aprovadas: cases.filter(caseItem => caseItem.status === 'APROVADA').length,
    rejeitadas: cases.filter(caseItem => caseItem.status === 'REJEITADA').length,
    falhas: cases.filter(caseItem => caseItem.status === 'FALHA_TECNICA').length,
  }
}

export function useCasesList(currentUser: User) {
  const [cases, setCases] = useState<CaseRequest[]>([])
  const [notifications, setNotifications] = useState<NotificationItem[]>([])
  const [loading, setLoading] = useState(true)
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('TODOS')
  const [searchQuery, setSearchQuery] = useState('')

  const loadData = useCallback(async () => {
    try {
      setLoading(true)
      const [caseData, notificationData] = await Promise.all([
        ApiService.getCases(currentUser),
        ApiService.getNotifications(currentUser),
      ])
      setCases(caseData)
      setNotifications(notificationData)
    } catch (error) {
      console.error('Erro ao carregar dados:', error)
    } finally {
      setLoading(false)
    }
  }, [currentUser])

  useEffect(() => {
    void loadData()
  }, [loadData])

  const createCase = useCallback(async (payload: CreateCasePayload) => {
    const created = await ApiService.createCase(payload, currentUser)
    await loadData()
    return created
  }, [currentUser, loadData])

  const markNotificationRead = useCallback(async (id: string) => {
    await ApiService.markNotificationRead(id, currentUser)
    setNotifications(await ApiService.getNotifications(currentUser))
  }, [currentUser])

  const filteredCases = useMemo(
    () => filterCases(cases, statusFilter, searchQuery),
    [cases, statusFilter, searchQuery],
  )
  const stats = useMemo(() => computeStats(cases), [cases])
  const unreadCount = notifications.filter(notification => !notification.readAt).length

  return {
    cases,
    notifications,
    loading,
    statusFilter,
    setStatusFilter,
    searchQuery,
    setSearchQuery,
    filteredCases,
    stats,
    unreadCount,
    loadData,
    createCase,
    markNotificationRead,
  }
}
