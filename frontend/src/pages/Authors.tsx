import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuthors } from '../hooks/useAuthors'
import SearchInput from '../components/SearchInput'
import Loading from '../components/Loading'
import ErrorState from '../components/ErrorState'
import Pagination from '../components/Pagination'
import { formatYear } from '../lib/utils'

export default function Authors() {
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const { data, isLoading, isError, error } = useAuthors(page, 20)

  const results = useMemo(() => {
    if (!data) return []
    const q = query.trim().toLowerCase()
    if (!q) return data.content
    return data.content.filter(
      (a) =>
        a.name.toLowerCase().includes(q) ||
        (a.birthYear !== null && String(a.birthYear).includes(q)),
    )
  }, [data, query])

  return (
    <main className="mx-auto max-w-[720px] px-8 py-16">
      <div className="mb-12">
        <h1 className="mb-2 font-serif text-[clamp(2rem,4vw,2.75rem)] font-normal tracking-[-0.02em] text-ink">
          Autores
        </h1>
        <p className="text-[15px] text-ink-muted">Explore os pensadores por trás das palavras.</p>
      </div>

      <div className="mb-10">
        <SearchInput value={query} onChange={(value) => { setQuery(value); setPage(0) }} placeholder="Buscar autores..." />
      </div>

      {isLoading && <Loading />}
      {isError && <ErrorState message={error instanceof Error ? error.message : 'Erro desconhecido'} />}

      <div className="flex flex-col">
        {results.map((author, i) => (
          <Link
            key={author.id}
            to={`/autores/${author.id}`}
            className={`flex items-baseline justify-between gap-8 py-6 transition-opacity hover:opacity-60 ${
              i === 0 ? '' : 'border-t border-hair-subtle'
            }`}
          >
            <div>
              <p className="mb-0.5 font-serif text-lg text-ink">{author.name}</p>
              <p className="text-[13px] text-ink-muted">
                {author.birthYear !== null && author.deathYear !== null
                  ? `${formatYear(author.birthYear)}–${formatYear(author.deathYear)}`
                  : ''}
              </p>
            </div>
            <span className="whitespace-nowrap text-xs text-ink-faint">
              {author.phraseCount} {author.phraseCount === 1 ? 'frase' : 'frases'}
            </span>
          </Link>
        ))}
      </div>
      {data && <Pagination data={data} onPage={setPage} />}
    </main>
  )
}
