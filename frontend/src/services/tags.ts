import { del, get, post, put } from './api'
import type { Paged, Tag, TagPayload } from '../types/models'

export function getTags(page = 0, size = 100): Promise<Paged<Tag>> {
  return get<Paged<Tag>>(`/tags?page=${page}&size=${size}`)
}

export function createTag(body: TagPayload): Promise<Tag> {
  return post<Tag>('/tags', body)
}

export function updateTag(id: number, body: TagPayload): Promise<Tag> {
  return put<Tag>(`/tags/${id}`, body)
}

export function deleteTag(id: number): Promise<void> {
  return del<void>(`/tags/${id}`)
}
