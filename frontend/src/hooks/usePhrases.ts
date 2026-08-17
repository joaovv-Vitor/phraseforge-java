import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createPhrase,
  deletePhrase,
  getPhrase,
  getPhrases,
  getRandomPhrase,
  updatePhrase,
  type PhrasePayload,
} from '../services/phrases'
import type { PhraseFilters } from '../types/models'

const PHRASE_KEYS = {
  all: ['phrases'] as const,
  list: (filters: PhraseFilters) => ['phrases', 'list', filters] as const,
  detail: (id: number) => ['phrases', 'detail', id] as const,
  random: ['phrases', 'random'] as const,
}

export function usePhrases(filters: PhraseFilters = {}) {
  return useQuery({
    queryKey: PHRASE_KEYS.list(filters),
    queryFn: () => getPhrases(filters),
  })
}

export function usePhrase(id: number) {
  return useQuery({
    queryKey: PHRASE_KEYS.detail(id),
    queryFn: () => getPhrase(id),
  })
}

export function useRandomPhrase() {
  return useQuery({
    queryKey: PHRASE_KEYS.random,
    queryFn: getRandomPhrase,
  })
}

export function useCreatePhrase() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: PhrasePayload) => createPhrase(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['phrases'] })
    },
  })
}

export function useUpdatePhrase(id: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: PhrasePayload) => updatePhrase(id, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['phrases'] })
    },
  })
}

export function useDeletePhrase() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deletePhrase(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['phrases'] })
      qc.invalidateQueries({ queryKey: ['authors'] })
    },
  })
}
