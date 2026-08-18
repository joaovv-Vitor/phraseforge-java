import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from './useAuth'
import type { UserRole } from '../services/auth'

export function RequireRole({ role, children }: { role: UserRole; children: ReactNode }) {
  const { status, user } = useAuth()
  const location = useLocation()

  if (status === 'restoring') {
    return <main className="flex min-h-screen items-center justify-center text-sm text-ink-faint">Restaurando sessão…</main>
  }
  if (status === 'anonymous') {
    return <Navigate to="/login" replace state={{ from: `${location.pathname}${location.search}` }} />
  }
  if (user?.role !== role) {
    return <Navigate to="/acesso-negado" replace />
  }
  return children
}
