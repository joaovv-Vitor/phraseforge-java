import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useCategory, useCategoryPhrases } from '../hooks/useCategories'
import QuoteCard from '../components/QuoteCard'
import Pagination from '../components/Pagination'
import EmptyState from '../components/EmptyState'
import Loading from '../components/Loading'
import ErrorState from '../components/ErrorState'

export default function CategoryDetail() {
  const { id } = useParams<{ id: string }>()
  const categoryId = Number(id)
  const [page, setPage] = useState(0)

  const { data: category, isLoading, isError, error } = useCategory(categoryId)
  const phrasesQuery = useCategoryPhrases(categoryId, page, 12)

  if (isLoading) return <main className="mx-auto max-w-[1040px] px-8 py-20"><Loading /></main>
  if (isError || !category) {
    return <main className="mx-auto max-w-[1040px] px-8 py-20"><ErrorState message={error instanceof Error ? error.message : 'Categoria não encontrada'} /></main>
  }

  return (
    <main className="mx-auto max-w-[1040px] px-8 py-16">
      <Link to="/categorias" className="mb-12 inline-flex items-center gap-1.5 text-[13px] text-ink-faint transition-colors hover:text-ink">
        ← Categorias
      </Link>

      <div className="mb-10">
        <h1 className="mb-2 font-serif text-[clamp(2rem,4vw,2.75rem)] font-normal tracking-[-0.02em] text-ink">
          {category.name}
        </h1>
        {category.description && (
          <p className="text-[15px] text-ink-muted">{category.description}</p>
        )}
      </div>

      <p className="mb-8 text-[13px] text-ink-faint">
        {category.phraseCount} {category.phraseCount === 1 ? 'frase' : 'frases'}
      </p>

      {phrasesQuery.isLoading && <Loading />}
      {phrasesQuery.isError && <ErrorState message="Não foi possível carregar as frases." />}
      {phrasesQuery.data && phrasesQuery.data.content.length === 0 && (
        <EmptyState title="Nenhuma frase nesta categoria" />
      )}

      {phrasesQuery.data && phrasesQuery.data.content.length > 0 && (
        <div className="grid grid-cols-1 gap-px overflow-hidden rounded border border-hair-subtle bg-hair-subtle sm:grid-cols-2 lg:grid-cols-3">
          {phrasesQuery.data.content.map((phrase) => (
            <QuoteCard key={phrase.id} phrase={phrase} />
          ))}
        </div>
      )}

      {phrasesQuery.data && <Pagination data={phrasesQuery.data} onPage={setPage} />}
    </main>
  )
}
