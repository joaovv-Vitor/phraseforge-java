import { Link } from 'react-router-dom'
import type { Phrase } from '../types/models'
import { formatYear } from '../lib/utils'
import { useCopy } from '../lib/useCopy'

export default function QuoteCard({ phrase }: { phrase: Phrase }) {
  const { copiedId, copy } = useCopy()
  const label = phrase.categories.length > 0 ? phrase.categories[0].name : ''

  const handleCopy = async () => {
    await copy(`"${phrase.content}" — ${phrase.author.name}`, String(phrase.id))
  }

  return (
    <div className="group flex flex-col gap-5 bg-paper p-8 transition-colors hover:bg-card">
      <Link to={`/frases/${phrase.id}`} className="flex flex-1 flex-col gap-5">
        <p className="font-serif italic leading-[1.65] text-ink">“{phrase.content}”</p>
        <div>
          <p className="mb-0.5 text-sm font-medium text-ink">— {phrase.author.name}</p>
          <p className="text-xs text-ink-faint">
            {label}
            {phrase.year !== null ? ` · ${formatYear(phrase.year)}` : ''}
          </p>
        </div>
      </Link>
      <button
        onClick={handleCopy}
        className="self-start text-xs text-ink-faint transition-colors hover:text-ink"
      >
        {copiedId === String(phrase.id) ? '✓ Copiada' : '⎘ Copiar'}
      </button>
    </div>
  )
}
