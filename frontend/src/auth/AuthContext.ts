import { createContext } from 'react'
import type { AuthenticatedUser, LoginPayload, RegisterPayload } from '../services/auth'

export type AuthStatus = 'restoring' | 'anonymous' | 'authenticated'

export interface AuthContextValue {
  status: AuthStatus
  user: AuthenticatedUser | null
  login: (payload: LoginPayload) => Promise<void>
  register: (payload: RegisterPayload) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
