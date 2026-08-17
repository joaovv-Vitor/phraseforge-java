import { del, get, post, put } from './api'
import type { Paged, Phrase, PhraseFilters, PhrasePayload } from '../types/models'

function buildQuery(filters: PhraseFilters): string {
  const params = new URLSearchParams()
  if (filters.query) params.set('query', filters.query)
  if (filters.authorId) params.set('authorId', String(filters.authorId))
  if (filters.categoryId) params.set('categoryId', String(filters.categoryId))
  if (filters.tagId) params.set('tagId', String(filters.tagId))
  if (filters.language) params.set('language', filters.language)
  params.set('page', String(filters.page ?? 0))
  params.set('size', String(filters.size ?? 20))
  return params.toString()
}

export function getPhrases(filters: PhraseFilters = {}): Promise<Paged<Phrase>> {
  return get<Paged<Phrase>>(`/phrases?${buildQuery(filters)}`)
}

export function getPhrase(id: number): Promise<Phrase> {
  return get<Phrase>(`/phrases/${id}`)
}

export function getRandomPhrase(): Promise<Phrase> {
  return get<Phrase>('/phrases/random')
}

export function createPhrase(body: PhrasePayload): Promise<Phrase> {
  return post<Phrase>('/phrases', body)
}

export function updatePhrase(id: number, body: PhrasePayload): Promise<Phrase> {
  return put<Phrase>(`/phrases/${id}`, body)
}

export function deletePhrase(id: number): Promise<void> {
  return del<void>(`/phrases/${id}`)
}
