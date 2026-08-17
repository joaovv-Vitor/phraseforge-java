import { Link } from 'react-router-dom'
import { usePhrases } from '../../hooks/usePhrases'
import { useAuthors } from '../../hooks/useAuthors'
import { useCategories } from '../../hooks/useCategories'
import { useTags } from '../../hooks/useTags'
import StatCard from '../../components/admin/StatCard'
import Loading from '../../components/Loading'

export default function Dashboard() {
  const phrases = usePhrases({ page: 0, size: 1 })
  const authors = useAuthors(0, 1)
  const categories = useCategories(0, 1)
  const tags = useTags(0, 1)
  const recent = usePhrases({ page: 0, size: 5 })

  return (
    <div>
      <h2 className="mb-8 font-serif text-[1.75rem] font-normal text-ink">Painel</h2>

      <div className="mb-10 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Total de Frases" value={phrases.data?.totalElements ?? '—'} />
        <StatCard label="Autores" value={authors.data?.totalElements ?? '—'} />
        <StatCard label="Categorias" value={categories.data?.totalElements ?? '—'} />
        <StatCard label="Tags" value={tags.data?.totalElements ?? '—'} />
      </div>

      <div className="overflow-hidden rounded border border-hair">
        <div className="flex items-center justify-between border-b border-hair-subtle px-5 py-4">
          <p className="text-sm font-medium text-ink">Frases Recentes</p>
          <Link to="/admin/frases" className="text-[13px] text-ink-muted hover:text-ink">
            Ver todas →
          </Link>
        </div>
        {recent.isLoading && <Loading />}
        {recent.data?.content.map((p, i) => (
          <div key={p.id}
            className={`flex justify-between gap-4 px-5 py-3.5 ${i < (recent.data?.content.length ?? 1) - 1 ? 'border-b border-hair-subtle' : ''}`}>
            <p className="flex-1 truncate text-sm text-ink">“{p.content}”</p>
            <p className="shrink-0 text-[13px] text-ink-faint">{p.author.name}</p>
          </div>
        ))}
      </div>
    </div>
  )
}
