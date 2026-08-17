import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createTag,
  deleteTag,
  getTags,
  updateTag,
  type TagPayload,
} from '../services/tags'

export function useTags(page = 0, size = 100) {
  return useQuery({
    queryKey: ['tags', 'list', page, size],
    queryFn: () => getTags(page, size),
  })
}

export function useCreateTag() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: TagPayload) => createTag(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['tags'] }),
  })
}

export function useUpdateTag(id: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: TagPayload) => updateTag(id, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['tags'] }),
  })
}

export function useDeleteTag() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteTag(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['tags'] }),
  })
}
