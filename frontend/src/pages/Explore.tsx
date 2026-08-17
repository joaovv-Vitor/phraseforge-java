import { useState } from 'react'
import { useCategories } from '../hooks/useCategories'
import { usePhrases } from '../hooks/usePhrases'
import SearchInput from '../components/SearchInput'
import PillButton from '../components/PillButton'
import QuoteCard from '../components/QuoteCard'
import Pagination from '../components/Pagination'
import EmptyState from '../components/EmptyState'
import Loading from '../components/Loading'
import ErrorState from '../components/ErrorState'

const PAGE_SIZE = 12

export default function Explore() {
  const [query, setQuery] = useState('')
  const [categoryId, setCategoryId] = useState<number | null>(null)
  const [page, setPage] = useState(0)

  const { data: categories } = useCategories(0, 50)
  const { data, isLoading, isError, error } = usePhrases({
    query: query || undefined,
    categoryId: categoryId ?? undefined,
    page,
    size: PAGE_SIZE,
  })

  const countLabel = data
    ? `${data.totalElements} ${data.totalElements === 1 ? 'frase' : 'frases'}`
    : ''

  return (
    <main className="mx-auto max-w-[1040px] px-8 py-16">
      <div className="mb-12">
        <h1 className="mb-2 font-serif text-[clamp(2rem,4vw,2.75rem)] font-normal tracking-[-0.02em] text-ink">
          Explorar
        </h1>
        <p className="text-[15px] text-ink-muted">
          Descubra ideias, pensamentos e palavras de diferentes pensadores.
        </p>
      </div>

      <div className="mb-7">
        <SearchInput
          value={query}
          onChange={(v) => {
            setQuery(v)
            setPage(0)
          }}
          placeholder="Buscar frases, autores ou temas..."
        />
      </div>

      {categories && categories.content.length > 0 && (
        <div className="mb-10 flex flex-wrap gap-2">
          <PillButton active={categoryId === null} onClick={() => { setCategoryId(null); setPage(0) }}>
            Todas
          </PillButton>
          {categories.content.map((c) => (
            <PillButton
              key={c.id}
              active={categoryId === c.id}
              onClick={() => { setCategoryId(c.id); setPage(0) }}
            >
              {c.name}
            </PillButton>
          ))}
        </div>
      )}

      <p className="mb-8 text-[13px] text-ink-faint">{countLabel}</p>

      {isLoading && <Loading />}
      {isError && <ErrorState message={error instanceof Error ? error.message : 'Erro desconhecido'} />}

      {data && data.content.length === 0 && (
        <EmptyState title="Nenhuma frase encontrada" subtitle="Tente outra busca ou filtro." />
      )}

      {data && data.content.length > 0 && (
        <div className="grid grid-cols-1 gap-px overflow-hidden rounded border border-hair-subtle bg-hair-subtle sm:grid-cols-2 lg:grid-cols-3">
          {data.content.map((phrase) => (
            <QuoteCard key={phrase.id} phrase={phrase} />
          ))}
        </div>
      )}

      {data && <Pagination data={data} onPage={setPage} />}
    </main>
  )
}
