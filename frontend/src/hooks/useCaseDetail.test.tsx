import { act, renderHook, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { CaseRequest, User } from '../types'

const api = vi.hoisted(() => ({
  getCaseById: vi.fn(),
  getHistory: vi.fn(),
}))

vi.mock('../services/api', () => ({ ApiService: api }))

import { useCaseDetail } from './useCaseDetail'

const user: User = {
  id: 'user-1',
  email: 'user@example.com',
  fullName: 'User',
  role: 'ROLE_USER',
}

const caseRequest: CaseRequest = {
  id: 'case-1',
  protocol: 'CF-20261003-1234',
  ownerSubject: user.id,
  ownerEmail: user.email,
  title: 'Case title',
  description: 'Case description long enough for the fixture',
  caseType: 'ANALISE_DOCUMENTAL',
  status: 'RASCUNHO',
  version: 1,
  processingRun: 0,
  rulesVersion: 'DOCUMENTAL_V1',
  submittedAt: null,
  createdAt: '2026-10-03T00:00:00Z',
  updatedAt: '2026-10-03T00:00:00Z',
  documents: [],
}

describe('useCaseDetail', () => {
  afterEach(() => vi.clearAllMocks())

  it('loads the case and its history for the selected user', async () => {
    api.getCaseById.mockResolvedValue(caseRequest)
    api.getHistory.mockResolvedValue([])

    const { result } = renderHook(() => useCaseDetail('case-1', user, vi.fn()))

    await waitFor(() => expect(result.current.caseData).toEqual(caseRequest))
    expect(result.current.histories).toEqual([])
    expect(result.current.loading).toBe(false)
    expect(api.getCaseById).toHaveBeenCalledWith('case-1', user)
    expect(api.getHistory).toHaveBeenCalledWith('case-1', user)
  })

  it('polls again while the case is processing and stops after unmount', async () => {
    api.getCaseById.mockResolvedValue({ ...caseRequest, status: 'PROCESSANDO' })
    api.getHistory.mockResolvedValue([])
    const { unmount } = renderHook(() => useCaseDetail('case-1', user, vi.fn()))

    await waitFor(() => expect(api.getCaseById).toHaveBeenCalledTimes(2), { timeout: 3000 })
    unmount()
  })
})
