import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { useToggleFavorite } from '../hooks/useFavorites'

export default function FavoriteButton({ phraseId, favorited, compact = false }: { phraseId: number; favorited: boolean; compact?: boolean }) {
  const { status } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const toggleFavorite = useToggleFavorite()

  function toggle() {
    if (status === 'restoring') return
    if (status === 'anonymous') {
      navigate('/login', { state: { from: `${location.pathname}${location.search}` } })
      return
    }
    toggleFavorite.mutate({ phraseId, favorited })
  }

  return (
    <button
      onClick={toggle}
      disabled={status === 'restoring' || toggleFavorite.isPending}
      aria-label={favorited ? 'Remover dos favoritos' : 'Adicionar aos favoritos'}
      className={compact
        ? `self-start text-xs transition-colors ${favorited ? 'text-ink' : 'text-ink-faint hover:text-ink'} disabled:opacity-50`
        : `rounded border px-4 py-2 text-[13px] transition-all ${favorited ? 'border-ink bg-ink font-medium text-paper' : 'border-hair text-ink-muted hover:border-ink-muted hover:text-ink'} disabled:opacity-50`}
    >
      {compact ? (favorited ? '♥ Favorita' : '♡ Favoritar') : (favorited ? '♥ Favorita' : '♡ Favoritar')}
    </button>
  )
}
