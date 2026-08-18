import { del, post } from './api'

export type UserRole = 'USER' | 'ADMIN'

export interface AuthenticatedUser {
  id: number
  email: string
  displayName: string
  role: UserRole
}

export interface AuthResponse {
  accessToken: string
  expiresIn: number
  user: AuthenticatedUser
}

export interface LoginPayload {
  email: string
  password: string
}

export interface RegisterPayload extends LoginPayload {
  displayName: string
}

export function login(payload: LoginPayload) {
  return post<AuthResponse>('/auth/login', payload)
}

export function register(payload: RegisterPayload) {
  return post<AuthResponse>('/auth/register', payload)
}

export function refresh() {
  return post<AuthResponse>('/auth/refresh', {})
}

export function logout() {
  return del<void>('/auth/logout')
}
