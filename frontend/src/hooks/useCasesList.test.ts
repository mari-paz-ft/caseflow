import { describe, expect, it } from 'vitest'
import { CaseRequest } from '../types'
import { computeStats, filterCases, parseStatusFilter } from './useCasesList'

const sampleCases: CaseRequest[] = [
  {
    id: 'draft', protocol: 'CF-20261001-1001', ownerSubject: 'user', ownerEmail: 'user@example.com',
    title: 'Rascunho Alpha', description: 'Cadastro inicial de fornecedor', caseType: 'ANALISE_DOCUMENTAL',
    status: 'RASCUNHO', version: 1, processingRun: 0, rulesVersion: 'DOCUMENTAL_V1',
    submittedAt: null, createdAt: '2026-10-01T00:00:00Z', updatedAt: '2026-10-01T00:00:00Z', documents: [],
  },
  {
    id: 'approved', protocol: 'CF-20261002-1002', ownerSubject: 'user', ownerEmail: 'user@example.com',
    title: 'Beta', description: 'Solicitação concluída', caseType: 'ANALISE_DOCUMENTAL',
    status: 'APROVADA', version: 2, processingRun: 1, rulesVersion: 'DOCUMENTAL_V1',
    submittedAt: '2026-10-02T00:00:00Z', createdAt: '2026-10-02T00:00:00Z', updatedAt: '2026-10-02T00:00:00Z', documents: [],
  },
]

describe('useCasesList pure helpers', () => {
  it('filters by status and case-insensitive text', () => {
    expect(filterCases(sampleCases, 'RASCUNHO', 'ALPHA').map(caseItem => caseItem.id)).toEqual(['draft'])
    expect(filterCases(sampleCases, 'TODOS', '1002').map(caseItem => caseItem.id)).toEqual(['approved'])
  })

  it('computes status totals', () => {
    expect(computeStats(sampleCases)).toEqual({ total: 2, rascunho: 1, aprovadas: 1, rejeitadas: 0, falhas: 0 })
  })

  it('parses known filter values without an any cast', () => {
    expect(parseStatusFilter('TODOS')).toBe('TODOS')
    expect(parseStatusFilter('APROVADA')).toBe('APROVADA')
    expect(parseStatusFilter('unknown')).toBeNull()
  })
})
