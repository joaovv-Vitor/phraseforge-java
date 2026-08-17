import { Link } from 'react-router-dom'
import { useCategories } from '../hooks/useCategories'
import Loading from '../components/Loading'
import ErrorState from '../components/ErrorState'
import Pagination from '../components/Pagination'
import { useState } from 'react'

export default function Categories() {
  const [page, setPage] = useState(0)
  const { data, isLoading, isError, error } = useCategories(page, 20)

  return (
    <main className="mx-auto max-w-[680px] px-8 py-16">
      <div className="mb-14">
        <h1 className="font-serif text-[clamp(2rem,4vw,2.75rem)] font-normal tracking-[-0.02em] text-ink">
          Categorias
        </h1>
      </div>

      {isLoading && <Loading />}
      {isError && <ErrorState message={error instanceof Error ? error.message : 'Erro desconhecido'} />}

      <div className="flex flex-col">
        {data?.content.map((category, i) => (
          <Link
            key={category.id}
            to={`/categorias/${category.id}`}
            className={`flex items-center justify-between py-5 transition-opacity hover:opacity-50 ${
              i === 0 ? '' : 'border-t border-hair-subtle'
            }`}
          >
            <div>
              <p className="font-serif text-xl font-normal tracking-[-0.01em] text-ink">
                {category.name}
              </p>
            </div>
            <div className="flex items-center gap-4">
              <span className="text-xs text-ink-faint">
                {category.phraseCount} {category.phraseCount === 1 ? 'frase' : 'frases'}
              </span>
              <span className="text-sm text-ink-faint">→</span>
            </div>
          </Link>
        ))}
      </div>
      {data && <Pagination data={data} onPage={setPage} />}
    </main>
  )
}
