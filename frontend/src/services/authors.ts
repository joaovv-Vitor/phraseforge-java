import { del, get, post, put } from './api'
import type { Author, AuthorPayload, AuthorSummary, Paged, PhraseSummary } from '../types/models'

export type { AuthorPayload } from '../types/models'

export function getAuthors(page = 0, size = 20): Promise<Paged<AuthorSummary>> {
  return get<Paged<AuthorSummary>>(`/authors?page=${page}&size=${size}`)
}

export function getAuthor(id: number): Promise<Author> {
  return get<Author>(`/authors/${id}`)
}

export function getAuthorPhrases(id: number, page = 0, size = 20): Promise<Paged<PhraseSummary>> {
  return get<Paged<PhraseSummary>>(`/authors/${id}/phrases?page=${page}&size=${size}`)
}

export function createAuthor(body: AuthorPayload): Promise<Author> {
  return post<Author>('/authors', body)
}

export function updateAuthor(id: number, body: AuthorPayload): Promise<Author> {
  return put<Author>(`/authors/${id}`, body)
}

export function deleteAuthor(id: number): Promise<void> {
  return del<void>(`/authors/${id}`)
}
