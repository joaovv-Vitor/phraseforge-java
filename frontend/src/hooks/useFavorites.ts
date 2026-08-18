import { useMutation, useQuery, useQueryClient, type QueryKey } from '@tanstack/react-query'
import { addFavorite, getFavorites, removeFavorite } from '../services/favorites'
import type { PhraseSummary } from '../types/models'

type FavoriteChange = { phraseId: number; favorited: boolean }
type CacheSnapshot = [QueryKey, unknown][]

function updateFavorited(data: unknown, phraseId: number, favorited: boolean): unknown {
  if (!data || typeof data !== 'object') return data
  if ('content' in data && Array.isArray(data.content)) {
    return {
      ...data,
      content: data.content.map((phrase: PhraseSummary) => phrase.id === phraseId ? { ...phrase, favorited } : phrase),
    }
  }
  if ('id' in data && data.id === phraseId) {
    return { ...data, favorited }
  }
  return data
}

function updateFavoritesList(data: unknown, phraseId: number, wasFavorited: boolean): unknown {
  if (!data || typeof data !== 'object' || !('content' in data) || !Array.isArray(data.content)) return data
  if (wasFavorited) {
    return { ...data, content: data.content.filter((phrase: PhraseSummary) => phrase.id !== phraseId) }
  }
  return data
}

export function useFavorites(page = 0, size = 20) {
  return useQuery({
    queryKey: ['favorites', page, size],
    queryFn: () => getFavorites(page, size),
  })
}

export function useToggleFavorite() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ phraseId, favorited }: FavoriteChange) => favorited ? removeFavorite(phraseId) : addFavorite(phraseId),
    onMutate: async ({ phraseId, favorited }) => {
      await Promise.all([
        queryClient.cancelQueries({ queryKey: ['phrases'] }),
        queryClient.cancelQueries({ queryKey: ['favorites'] }),
      ])
      const phrases = queryClient.getQueriesData({ queryKey: ['phrases'] })
      const favorites = queryClient.getQueriesData({ queryKey: ['favorites'] })
      queryClient.setQueriesData({ queryKey: ['phrases'] }, (data) => updateFavorited(data, phraseId, !favorited))
      queryClient.setQueriesData({ queryKey: ['favorites'] }, (data) => updateFavoritesList(data, phraseId, favorited))
      return { phrases, favorites } satisfies { phrases: CacheSnapshot; favorites: CacheSnapshot }
    },
    onError: (_error, _variables, snapshot) => {
      snapshot?.phrases.forEach(([key, data]) => queryClient.setQueryData(key, data))
      snapshot?.favorites.forEach(([key, data]) => queryClient.setQueryData(key, data))
    },
    onSettled: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['phrases'] }),
        queryClient.invalidateQueries({ queryKey: ['favorites'] }),
      ])
    },
  })
}
