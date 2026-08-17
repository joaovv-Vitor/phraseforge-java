import { Link, useParams } from 'react-router-dom'
import { useAuthor, useAuthorPhrases } from '../hooks/useAuthors'
import { formatYear } from '../lib/utils'
import Loading from '../components/Loading'
import ErrorState from '../components/ErrorState'
import Pagination from '../components/Pagination'
import { useState } from 'react'

export default function AuthorDetail() {
  const { id } = useParams<{ id: string }>()
  const authorId = Number(id)
  const [page, setPage] = useState(0)

  const { data: author, isLoading, isError, error } = useAuthor(authorId)
  const phrasesQuery = useAuthorPhrases(authorId, page, 10)

  if (isLoading) return <main className="mx-auto max-w-[680px] px-8 py-20"><Loading /></main>
  if (isError || !author) {
    return <main className="mx-auto max-w-[680px] px-8 py-20"><ErrorState message={error instanceof Error ? error.message : 'Autor não encontrado'} /></main>
  }

  const years =
    author.birthYear !== null && author.deathYear !== null
      ? `${formatYear(author.birthYear)}–${formatYear(author.deathYear)}`
      : ''

  return (
    <main className="mx-auto max-w-[680px] px-8 py-20">
      <Link to="/autores" className="mb-12 inline-flex items-center gap-1.5 text-[13px] text-ink-faint transition-colors hover:text-ink">
        ← Autores
      </Link>

      <div className="mb-14 border-b border-hair-subtle pb-12">
        <h1 className="mb-2 font-serif text-[clamp(2rem,4vw,3rem)] font-normal tracking-[-0.02em] text-ink">
          {author.name}
        </h1>
        {years && <p className="mb-7 text-[13px] text-ink-faint">{years}</p>}
        {author.biography && (
          <p className="text-[15px] leading-[1.75] text-ink-muted">{author.biography}</p>
        )}
      </div>

      <p className="mb-6 text-xs font-semibold uppercase tracking-[0.08em] text-ink-faint">
        Frases de {author.name}
      </p>

      {phrasesQuery.isLoading && <Loading />}
      {phrasesQuery.isError && <ErrorState message="Não foi possível carregar as frases." />}

      {phrasesQuery.data && phrasesQuery.data.content.length === 0 && (
        <p className="py-8 text-sm text-ink-faint">Nenhuma frase registrada para este autor.</p>
      )}

      <div className="flex flex-col">
        {phrasesQuery.data?.content.map((phrase, i) => (
          <Link
            key={phrase.id}
            to={`/frases/${phrase.id}`}
            className={`flex flex-col gap-4 py-7 transition-opacity hover:opacity-60 ${
              i === 0 ? '' : 'border-t border-hair-subtle'
            }`}
          >
            <p className="font-serif text-[17px] italic leading-[1.65] text-ink">“{phrase.content}”</p>
            <div className="flex flex-wrap gap-2">
              {phrase.categories.map((c) => (
                <span key={c.id} className="rounded-[99px] border border-hair-subtle px-2.5 py-0.5 text-[11px] text-ink-faint">
                  {c.name}
                </span>
              ))}
            </div>
          </Link>
        ))}
      </div>

      {phrasesQuery.data && <Pagination data={phrasesQuery.data} onPage={setPage} />}
    </main>
  )
}
