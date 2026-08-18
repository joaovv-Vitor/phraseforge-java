import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { login as loginRequest, logout as logoutRequest, refresh, register as registerRequest, type AuthenticatedUser, type LoginPayload, type RegisterPayload } from '../services/auth'
import { setAccessToken, setRefreshHandler } from '../services/api'
import { AuthContext, type AuthStatus } from './AuthContext'

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [status, setStatus] = useState<AuthStatus>('restoring')
  const [user, setUser] = useState<AuthenticatedUser | null>(null)
  const refreshPromiseRef = useRef<Promise<string | null> | null>(null)

  const clearSession = useCallback(() => {
    setAccessToken(null)
    setUser(null)
    setStatus('anonymous')
    queryClient.removeQueries({ queryKey: ['favorites'] })
    void queryClient.invalidateQueries({ queryKey: ['phrases'] })
  }, [queryClient])

  const applySession = useCallback((response: Awaited<ReturnType<typeof refresh>>) => {
    setAccessToken(response.accessToken)
    setUser(response.user)
    setStatus('authenticated')
    void queryClient.invalidateQueries({ queryKey: ['phrases'] })
  }, [queryClient])

  const restoreSession = useCallback(() => {
    if (refreshPromiseRef.current) {
      return refreshPromiseRef.current
    }
    const pending = refresh()
      .then((response) => {
        applySession(response)
        return response.accessToken
      })
      .catch(() => {
        clearSession()
        return null
      })
      .finally(() => {
        refreshPromiseRef.current = null
      })
    refreshPromiseRef.current = pending
    return pending
  }, [applySession, clearSession])

  useEffect(() => {
    setRefreshHandler(restoreSession)
    void restoreSession()
    return () => setRefreshHandler(null)
  }, [restoreSession])

  const login = useCallback(async (payload: LoginPayload) => {
    const response = await loginRequest(payload)
    applySession(response)
  }, [applySession])

  const register = useCallback(async (payload: RegisterPayload) => {
    const response = await registerRequest(payload)
    applySession(response)
  }, [applySession])

  const logout = useCallback(async () => {
    try {
      await logoutRequest()
    } finally {
      clearSession()
    }
  }, [clearSession])

  const value = useMemo(() => ({ status, user, login, register, logout }), [status, user, login, register, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
