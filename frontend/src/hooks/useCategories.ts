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

export function useCategories(page = 0, size = 50) {
  return useQuery({
    queryKey: ['categories', 'list', page, size],
    queryFn: () => getCategories(page, size),
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
