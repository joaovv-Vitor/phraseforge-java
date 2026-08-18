import { Link, useParams } from 'react-router-dom'
import { usePhrase, usePhrases } from '../hooks/usePhrases'
import { useCopy } from '../lib/useCopy'
import { formatYear } from '../lib/utils'
import Chip from '../components/Chip'
import Loading from '../components/Loading'
import ErrorState from '../components/ErrorState'
import FavoriteButton from '../components/FavoriteButton'

export default function QuoteDetail() {
  const { id } = useParams<{ id: string }>()
  const phraseId = Number(id)
  const { data, isLoading, isError, error } = usePhrase(phraseId)
  const { copiedId, copy } = useCopy()

  const relatedQuery = usePhrases(
    data ? { authorId: data.author.id, size: 5 } : undefined,
  )
  const related = relatedQuery.data?.content.filter((p) => p.id !== phraseId) ?? []

  if (isLoading) return <main className="mx-auto max-w-[680px] px-8 py-20"><Loading /></main>
  if (isError || !data) {
    return <main className="mx-auto max-w-[680px] px-8 py-20"><ErrorState message={error instanceof Error ? error.message : 'Frase não encontrada'} /></main>
  }

  const quoteText = `"${data.content}" — ${data.author.name}`
  const label = data.categories.length > 0 ? data.categories[0].name : ''

  return (
    <main className="mx-auto max-w-[680px] px-8 py-20">
      <Link to="/explore" className="mb-12 inline-flex items-center gap-1.5 text-[13px] text-ink-faint transition-colors hover:text-ink">
        ← Voltar
      </Link>

      {label && (
        <p className="mb-8 text-[11px] font-semibold uppercase tracking-[0.1em] text-ink-faint">
          {label}
        </p>
      )}

      <blockquote className="mb-10 font-serif text-[clamp(1.375rem,3vw,2rem)] font-normal italic leading-[1.55] tracking-[-0.015em] text-ink">
        “{data.content}”
      </blockquote>

      <div className="mb-12">
        <Link to={`/autores/${data.author.id}`} className="mb-0.5 block text-base font-medium text-ink transition-opacity hover:opacity-60">
          — {data.author.name}
        </Link>
        <span className="text-[13px] text-ink-faint">{formatYear(data.year)}</span>
      </div>

      <div className="mb-12 flex flex-wrap gap-2">
        {data.categories.map((c) => (
          <Chip key={c.id}>{c.name}</Chip>
        ))}
        {data.tags.map((t) => (
          <Chip key={t.id}>#{t.name}</Chip>
        ))}
        <span className="self-center text-xs text-ink-faint">
          {data.language.toUpperCase()}
          {data.source ? ` · ${data.source}` : ''}
        </span>
      </div>

      <div className="mb-20 flex gap-3 border-t border-hair-subtle pt-8">
        <button
          onClick={() => copy(quoteText, 'quote')}
          className={`rounded border px-4 py-2 text-[13px] transition-all ${
            copiedId === 'quote' ? 'border-ink bg-ink font-medium text-paper' : 'border-hair text-ink-muted hover:border-ink-muted hover:text-ink'
          }`}
        >
          {copiedId === 'quote' ? '✓ Copiada' : '⎘ Copiar Frase'}
        </button>
        <FavoriteButton phraseId={data.id} favorited={data.favorited} />
        <button
          onClick={() => copy(window.location.href, 'share')}
          className="rounded border border-hair px-4 py-2 text-[13px] text-ink-muted transition-all hover:border-ink-muted hover:text-ink"
        >
          {copiedId === 'share' ? 'Link copiado' : '↗ Compartilhar'}
        </button>
      </div>

      {related.length > 0 && (
        <section>
          <p className="mb-6 text-xs font-semibold uppercase tracking-[0.08em] text-ink-faint">
            Mais de {data.author.name}
          </p>
          <div className="flex flex-col">
            {related.map((p) => (
              <Link
                key={p.id}
                to={`/frases/${p.id}`}
                className="border-b border-hair-subtle py-5 transition-opacity hover:opacity-60 first:border-t"
              >
                <p className="font-serif italic leading-[1.6] text-ink">“{p.content}”</p>
              </Link>
            ))}
          </div>
        </section>
      )}
    </main>
  )
}
