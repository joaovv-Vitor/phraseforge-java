import QuoteCard from '../components/QuoteCard'
import Loading from '../components/Loading'
import ErrorState from '../components/ErrorState'
import EmptyState from '../components/EmptyState'
import { useFavorites } from '../hooks/useFavorites'

export default function Favorites() {
  const { data, isLoading, isError, error } = useFavorites()

  if (isLoading) return <main className="mx-auto max-w-[1040px] px-8 py-20"><Loading /></main>
  if (isError || !data) return <main className="mx-auto max-w-[1040px] px-8 py-20"><ErrorState message={error instanceof Error ? error.message : 'Não foi possível carregar seus favoritos.'} /></main>

  return (
    <main className="mx-auto max-w-[1040px] px-8 py-14">
      <p className="mb-2 text-[11px] font-semibold uppercase tracking-[0.1em] text-ink-faint">Sua coleção</p>
      <h1 className="mb-10 font-serif text-3xl text-ink">Favoritos</h1>
      {data.content.length === 0 ? (
        <EmptyState title="Nenhuma frase favorita" subtitle="Quando encontrar uma frase especial, salve-a aqui." />
      ) : (
        <div className="grid border border-hair-subtle sm:grid-cols-2">
          {data.content.map((phrase) => <QuoteCard key={phrase.id} phrase={phrase} />)}
        </div>
      )}
    </main>
  )
}
