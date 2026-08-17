import { del, get, post, put } from './api'
import type { Category, CategoryPayload, CategorySummary, Paged, Phrase } from '../types/models'

export type { CategoryPayload } from '../types/models'

export function getCategories(page = 0, size = 50): Promise<Paged<CategorySummary>> {
  return get<Paged<CategorySummary>>(`/categories?page=${page}&size=${size}`)
}

export function getCategory(id: number): Promise<Category> {
  return get<Category>(`/categories/${id}`)
}

export function getCategoryPhrases(id: number, page = 0, size = 20): Promise<Paged<Phrase>> {
  return get<Paged<Phrase>>(`/categories/${id}/phrases?page=${page}&size=${size}`)
}

export function createCategory(body: CategoryPayload): Promise<Category> {
  return post<Category>('/categories', body)
}

export function updateCategory(id: number, body: CategoryPayload): Promise<Category> {
  return put<Category>(`/categories/${id}`, body)
}

export function deleteCategory(id: number): Promise<void> {
  return del<void>(`/categories/${id}`)
}
