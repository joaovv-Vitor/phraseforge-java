import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { useRandomPhrase } from '../hooks/usePhrases'
import { useCopy } from '../lib/useCopy'
import { formatYear } from '../lib/utils'

export default function Home() {
  const { data, isLoading, isError, refetch } = useRandomPhrase()
  const { copiedId, copy } = useCopy()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [visible, setVisible] = useState(true)

  const nextQuote = async () => {
    setVisible(false)
    await new Promise((r) => setTimeout(r, 280))
    queryClient.removeQueries({ queryKey: ['phrases', 'random'] })
    await refetch()
    setVisible(true)
  }

  if (isLoading) {
    return <main className="flex min-h-[calc(100vh-52px)] items-center justify-center text-sm text-ink-faint">Carregando…</main>
  }

  if (isError || !data) {
    return (
      <main className="flex min-h-[calc(100vh-52px)] items-center justify-center">
        <p className="text-sm text-ink-faint">Não foi possível carregar a frase.</p>
      </main>
    )
  }

  const phrase = data
  const label = phrase.categories.length > 0 ? phrase.categories[0].name : ''
  const quoteText = `"${phrase.content}" — ${phrase.author.name}`

  const copyQuote = () => copy(quoteText, String(phrase.id))
  const share = () => copy(window.location.href, 'share')

  return (
    <main className="flex min-h-[calc(100vh-52px)] flex-col items-center justify-center px-8 py-16">
      <div
        className={`w-full max-w-[680px] transition-opacity duration-300 ${visible ? 'opacity-100' : 'opacity-0'}`}
      >
        {label && (
          <p className="mb-10 text-[11px] font-semibold uppercase tracking-[0.1em] text-ink-faint">
            {label}
          </p>
        )}

        <blockquote className="mb-10 font-serif text-[clamp(1.375rem,3vw,2rem)] font-normal italic leading-[1.55] tracking-[-0.015em] text-ink">
          “{phrase.content}”
        </blockquote>

        <div className="mb-14">
          <button
            onClick={() => navigate(`/autores/${phrase.author.id}`)}
            className="mb-0.5 block text-left text-[15px] font-medium text-ink transition-opacity hover:opacity-60"
          >
            — {phrase.author.name}
          </button>
          <span className="text-[13px] text-ink-faint">{formatYear(phrase.year)}</span>
        </div>

        <div className="mb-16 flex items-center gap-6">
          <button onClick={copyQuote} className="flex items-center gap-1.5 text-[13px] text-ink-muted transition-colors hover:text-ink">
            <span>{copiedId === String(phrase.id) ? '✓' : '⎘'}</span>
            {copiedId === String(phrase.id) ? 'Copiada' : 'Copiar'}
          </button>
          <button onClick={share} className="flex items-center gap-1.5 text-[13px] text-ink-muted transition-colors hover:text-ink">
            <span>↗</span>
            {copiedId === 'share' ? 'Link copiado' : 'Compartilhar'}
          </button>
        </div>

        <div className="flex items-center gap-6">
          <button
            onClick={nextQuote}
            className="rounded-[99px] border border-hair px-5.5 py-2 text-[13px] font-medium text-ink transition-all hover:bg-ink hover:text-paper"
          >
            Nova Frase
          </button>
        </div>
      </div>
    </main>
  )
}
