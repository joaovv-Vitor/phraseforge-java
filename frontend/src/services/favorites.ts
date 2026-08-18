import { del, get, put } from './api'
import type { Paged, PhraseSummary } from '../types/models'

export function getFavorites(page = 0, size = 20) {
  return get<Paged<PhraseSummary>>(`/users/me/favorites?page=${page}&size=${size}`)
}

export function addFavorite(phraseId: number) {
  return put<void>(`/users/me/favorites/${phraseId}`, {})
}

export function removeFavorite(phraseId: number) {
  return del<void>(`/users/me/favorites/${phraseId}`)
}
