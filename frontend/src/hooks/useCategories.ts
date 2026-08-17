import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createCategory,
  deleteCategory,
  getCategories,
  getCategory,
  getCategoryPhrases,
  updateCategory,
  type CategoryPayload,
} from '../services/categories'
import type { CategorySummary } from '../types/models'

export function useCategories(page = 0, size = 50) {
  return useQuery({
    queryKey: ['categories', 'list', page, size],
    queryFn: () => getCategories(page, size),
  })
}

export function useAllCategories() {
  return useQuery({
    queryKey: ['categories', 'all'],
    queryFn: async (): Promise<CategorySummary[]> => {
      const first = await getCategories(0, 100)
      const rest = await Promise.all(
        Array.from({ length: Math.max(first.totalPages - 1, 0) }, (_, index) =>
          getCategories(index + 1, 100),
        ),
      )
      return [first, ...rest].flatMap((page) => page.content)
    },
  })
}

export function useCategory(id: number) {
  return useQuery({
    queryKey: ['categories', 'detail', id],
    queryFn: () => getCategory(id),
  })
}

export function useCategoryPhrases(id: number, page = 0, size = 20) {
  return useQuery({
    queryKey: ['categories', id, 'phrases', page, size],
    queryFn: () => getCategoryPhrases(id, page, size),
  })
}

export function useCreateCategory() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: CategoryPayload) => createCategory(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['categories'] }),
  })
}

export function useUpdateCategory(id: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: CategoryPayload) => updateCategory(id, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['categories'] }),
  })
}

export function useDeleteCategory() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteCategory(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['categories'] }),
  })
}
