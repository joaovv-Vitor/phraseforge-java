const BASE_URL = import.meta.env.VITE_API_URL ?? '/api/v1'

let accessToken: string | null = null
let refreshHandler: (() => Promise<string | null>) | null = null
let pendingRefresh: Promise<string | null> | null = null

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

export interface ApiErrorBody {
  status: number
  message: string
  timestamp: string
}

export function setAccessToken(token: string | null) {
  accessToken = token
}

export function setRefreshHandler(handler: (() => Promise<string | null>) | null) {
  refreshHandler = handler
}

async function request<T>(path: string, options: RequestInit = {}, retried = false): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Content-Type', 'application/json')
  if (accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`)
  }

  const res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    credentials: 'include',
    headers,
  })

  if (res.status === 401 && !retried && path !== '/auth/refresh' && refreshHandler) {
    pendingRefresh ??= refreshHandler().finally(() => {
      pendingRefresh = null
    })
    if (await pendingRefresh) {
      return request<T>(path, options, true)
    }
  }

  if (!res.ok) {
    let message = `Request failed with status ${res.status}`
    try {
      const body = (await res.json()) as ApiErrorBody
      message = body.message ?? message
    } catch {
      // non-JSON error body; keep generic message
    }
    throw new ApiError(res.status, message)
  }

  if (res.status === 204) {
    return undefined as T
  }

  return res.json() as Promise<T>
}

export function get<T>(path: string): Promise<T> {
  return request<T>(path)
}

export function post<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, { method: 'POST', body: JSON.stringify(body) })
}

export function put<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, { method: 'PUT', body: JSON.stringify(body) })
}

export function del<T>(path: string): Promise<T> {
  return request<T>(path, { method: 'DELETE' })
}
