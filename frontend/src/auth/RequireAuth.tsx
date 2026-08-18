import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from './useAuth'

export function RequireAuth({ children }: { children: ReactNode }) {
  const { status } = useAuth()
  const location = useLocation()

  if (status === 'restoring') {
    return <main className="flex min-h-screen items-center justify-center text-sm text-ink-faint">Restaurando sessão…</main>
  }
  if (status === 'anonymous') {
    return <Navigate to="/login" replace state={{ from: `${location.pathname}${location.search}` }} />
  }
  return children
}
