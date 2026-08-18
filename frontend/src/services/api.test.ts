import { afterEach, describe, expect, it, vi } from 'vitest'
import { get, setAccessToken, setRefreshHandler } from './api'

const unauthorized = () => new Response(JSON.stringify({
  status: 401,
  code: 'UNAUTHORIZED',
  message: 'Authentication is required',
  timestamp: '2026-08-17T12:00:00Z',
}), {
  status: 401,
  headers: { 'Content-Type': 'application/json' },
})

afterEach(() => {
  setAccessToken(null)
  setRefreshHandler(null)
  vi.unstubAllGlobals()
})

describe('API authentication retry', () => {
  it('shares one refresh between concurrent unauthorized requests', async () => {
    let releaseRefresh: (() => void) | undefined
    const refreshGate = new Promise<void>((resolve) => {
      releaseRefresh = resolve
    })
    const refresh = vi.fn(async () => {
      await refreshGate
      setAccessToken('fresh-token')
      return 'fresh-token'
    })
    const fetchMock = vi.fn(async (_input: RequestInfo | URL, init?: RequestInit) => {
      const authorization = new Headers(init?.headers).get('Authorization')
      if (authorization !== 'Bearer fresh-token') return unauthorized()
      return new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    })
    vi.stubGlobal('fetch', fetchMock)
    setAccessToken('expired-token')
    setRefreshHandler(refresh)

    const first = get<{ ok: boolean }>('/phrases')
    const second = get<{ ok: boolean }>('/users/me')
    await vi.waitFor(() => expect(refresh).toHaveBeenCalledOnce())
    releaseRefresh?.()

    await expect(Promise.all([first, second])).resolves.toEqual([{ ok: true }, { ok: true }])
    expect(refresh).toHaveBeenCalledOnce()
    expect(fetchMock).toHaveBeenCalledTimes(4)
  })

  it('retries only once when the renewed token is rejected', async () => {
    const fetchMock = vi.fn(async () => unauthorized())
    const refresh = vi.fn(async () => {
      setAccessToken('still-invalid')
      return 'still-invalid'
    })
    vi.stubGlobal('fetch', fetchMock)
    setAccessToken('expired-token')
    setRefreshHandler(refresh)

    await expect(get('/users/me')).rejects.toEqual(expect.objectContaining({
      status: 401,
      code: 'UNAUTHORIZED',
    }))
    expect(refresh).toHaveBeenCalledOnce()
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })
})
