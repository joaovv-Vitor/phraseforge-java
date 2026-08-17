import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createAuthor,
  deleteAuthor,
  getAuthor,
  getAuthorPhrases,
  getAuthors,
  updateAuthor,
  type AuthorPayload,
} from '../services/authors'

export function useAuthors(page = 0, size = 20) {
  return useQuery({
    queryKey: ['authors', 'list', page, size],
    queryFn: () => getAuthors(page, size),
  })
}

export function useAuthor(id: number) {
  return useQuery({
    queryKey: ['authors', 'detail', id],
    queryFn: () => getAuthor(id),
  })
}

export function useAuthorPhrases(id: number, page = 0, size = 20) {
  return useQuery({
    queryKey: ['authors', id, 'phrases', page, size],
    queryFn: () => getAuthorPhrases(id, page, size),
  })
}

export function useCreateAuthor() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: AuthorPayload) => createAuthor(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['authors'] }),
  })
}

export function useUpdateAuthor(id: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: AuthorPayload) => updateAuthor(id, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['authors'] }),
  })
}

export function useDeleteAuthor() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteAuthor(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['authors'] }),
  })
}
