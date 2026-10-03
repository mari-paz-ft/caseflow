import { afterEach, describe, expect, it, vi } from 'vitest'
import { withFallback } from './httpClient'

describe('withFallback', () => {
  afterEach(() => vi.restoreAllMocks())

  it('returns the backend response when the request succeeds', async () => {
    const fallback = vi.fn(() => 'mock')

    await expect(withFallback(true, async () => 'live', fallback)).resolves.toBe('live')
    expect(fallback).not.toHaveBeenCalled()
  })

  it('uses the fallback when the backend request fails', async () => {
    vi.spyOn(console, 'warn').mockImplementation(() => undefined)
    const fallback = vi.fn(() => 'mock')

    await expect(withFallback(true, async () => { throw new Error('offline') }, fallback)).resolves.toBe('mock')
    expect(fallback).toHaveBeenCalledOnce()
  })

  it('skips the backend when it is marked unavailable', async () => {
    const backend = vi.fn(async () => 'live')

    await expect(withFallback(false, backend, () => 'mock')).resolves.toBe('mock')
    expect(backend).not.toHaveBeenCalled()
  })
})
