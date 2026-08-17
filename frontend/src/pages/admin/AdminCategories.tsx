import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useCategories, useCreateCategory, useDeleteCategory, useUpdateCategory } from '../../hooks/useCategories'
import ActionBar from '../../components/admin/ActionBar'
import EntityTable from '../../components/admin/EntityTable'
import CategoryForm from '../../components/admin/CategoryForm'
import Loading from '../../components/Loading'
import Pagination from '../../components/Pagination'
import type { CategoryPayload, CategorySummary } from '../../types/models'

export default function AdminCategories() {
  const [page, setPage] = useState(0)
  const { data, isLoading } = useCategories(page, 20)
  const createCategory = useCreateCategory()
  const deleteCategory = useDeleteCategory()

  const [editing, setEditing] = useState<CategorySummary | null>(null)
  const [message, setMessage] = useState('')

  const flash = (msg: string) => {
    setMessage(msg)
    window.setTimeout(() => setMessage(''), 3000)
  }

  const updateCategory = useUpdateCategory(editing?.id ?? 0)

  const handleSubmit = async (payload: CategoryPayload) => {
    try {
      if (editing) {
        await updateCategory.mutateAsync(payload)
        flash('Categoria atualizada com sucesso.')
      } else {
        await createCategory.mutateAsync(payload)
        flash('Categoria salva com sucesso.')
      }
      setEditing(null)
    } catch (err) {
      flash(err instanceof Error ? err.message : 'Erro ao salvar.')
    }
  }

  return (
    <div className="max-w-[900px]">
      <ActionBar
        title="Categorias"
        action={
          <button onClick={() => setEditing(null)}
            className="rounded bg-ink px-4 py-2 text-[13px] font-medium text-paper transition-opacity hover:opacity-80">
            + Nova Categoria
          </button>
        }
      />

      {message && <div className="mb-6 rounded border border-hair bg-card px-4 py-3 text-sm text-ink-muted">{message}</div>}

      <CategoryForm
        key={editing?.id ?? 'new'}
        category={editing}
        submitLabel={editing ? 'Atualizar Categoria' : 'Salvar Categoria'}
        onSubmit={handleSubmit}
        onCancel={() => setEditing(null)}
      />

      {isLoading && <Loading />}

      <EntityTable>
        {data?.content.map((c, i) => (
          <div key={c.id}
            className={`flex items-center justify-between gap-4 px-5 py-4 transition-colors hover:bg-card ${i < (data?.content.length ?? 1) - 1 ? 'border-b border-hair-subtle' : ''}`}>
            <div>
              <p className="text-[15px] text-ink">{c.name}</p>
              <p className="text-[13px] text-ink-faint">{c.phraseCount} {c.phraseCount === 1 ? 'frase' : 'frases'}</p>
            </div>
            <div className="flex shrink-0 gap-3 text-xs">
              <Link to={`/categorias/${c.id}`} className="text-ink-muted hover:text-ink">Ver</Link>
              <button onClick={() => setEditing(c)} className="text-ink-muted hover:text-ink">Editar</button>
              <button onClick={() => { if (window.confirm('Excluir esta categoria?')) deleteCategory.mutate(c.id) }} className="text-ink-faint hover:text-ink">Excluir</button>
            </div>
          </div>
        ))}
      </EntityTable>
      {data && <Pagination data={data} onPage={setPage} />}
    </div>
  )
}
