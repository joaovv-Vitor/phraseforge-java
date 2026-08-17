import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createTag,
  deleteTag,
  getTags,
  updateTag,
  type TagPayload,
} from '../services/tags'
import type { Tag } from '../types/models'

export function useTags(page = 0, size = 100) {
  return useQuery({
    queryKey: ['tags', 'list', page, size],
    queryFn: () => getTags(page, size),
  })
}

export function useAllTags() {
  return useQuery({
    queryKey: ['tags', 'all'],
    queryFn: async (): Promise<Tag[]> => {
      const first = await getTags(0, 100)
      const rest = await Promise.all(
        Array.from({ length: Math.max(first.totalPages - 1, 0) }, (_, index) =>
          getTags(index + 1, 100),
        ),
      )
      return [first, ...rest].flatMap((page) => page.content)
    },
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
