import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useCreatePhrase, useDeletePhrase, usePhrases, useUpdatePhrase } from '../../hooks/usePhrases'
import { useAuthors } from '../../hooks/useAuthors'
import { useCategories } from '../../hooks/useCategories'
import { useTags } from '../../hooks/useTags'
import ActionBar from '../../components/admin/ActionBar'
import EntityTable from '../../components/admin/EntityTable'
import PhraseForm from '../../components/admin/PhraseForm'
import Loading from '../../components/Loading'
import type { PhrasePayload, PhraseSummary } from '../../types/models'

export default function AdminPhrases() {
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<PhraseSummary | null>(null)
  const [message, setMessage] = useState('')

  const { data, isLoading } = usePhrases({ page, size: 10 })
  const createPhrase = useCreatePhrase()
  const deletePhrase = useDeletePhrase()
  const updatePhrase = useUpdatePhrase(editing?.id ?? 0)

  const authors = useAuthors(0, 100)
  const categories = useCategories(0, 100)
  const tags = useTags(0, 100)

  const flash = (msg: string) => {
    setMessage(msg)
    window.setTimeout(() => setMessage(''), 3000)
  }

  const handleSubmit = async (payload: PhrasePayload) => {
    try {
      if (editing) {
        await updatePhrase.mutateAsync(payload)
        flash('Frase atualizada com sucesso.')
      } else {
        await createPhrase.mutateAsync(payload)
        flash('Frase salva com sucesso.')
      }
      setEditing(null)
    } catch (err) {
      flash(err instanceof Error ? err.message : 'Erro ao salvar.')
    }
  }

  const handleDelete = async (id: number) => {
    if (window.confirm('Excluir esta frase?')) {
      await deletePhrase.mutateAsync(id)
    }
  }

  return (
    <div className="max-w-[900px]">
      <ActionBar
        title="Frases"
        action={
          <button onClick={() => setEditing(null)}
            className="rounded bg-ink px-4 py-2 text-[13px] font-medium text-paper transition-opacity hover:opacity-80">
            + Nova Frase
          </button>
        }
      />

      {message && (
        <div className="mb-6 rounded border border-hair bg-card px-4 py-3 text-sm text-ink-muted">{message}</div>
      )}

      <PhraseForm
        key={editing?.id ?? 'new'}
        phrase={editing}
        authors={authors.data?.content ?? []}
        categories={categories.data?.content ?? []}
        tags={tags.data?.content ?? []}
        submitLabel={editing ? 'Atualizar Frase' : 'Salvar Frase'}
        onSubmit={handleSubmit}
        onCancel={() => setEditing(null)}
      />

      {isLoading && <Loading />}

      <EntityTable>
        {data?.content.map((p, i) => (
          <div key={p.id}
            className={`flex items-center justify-between gap-4 px-5 py-4 transition-colors hover:bg-card ${i < (data?.content.length ?? 1) - 1 ? 'border-b border-hair-subtle' : ''}`}>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm text-ink">“{p.content}”</p>
              <p className="text-[13px] text-ink-faint">{p.author.name} · {p.language.toUpperCase()}</p>
            </div>
            <div className="flex shrink-0 gap-3 text-xs">
              <Link to={`/frases/${p.id}`} className="text-ink-muted hover:text-ink">Ver</Link>
              <button onClick={() => setEditing(p)} className="text-ink-muted hover:text-ink">Editar</button>
              <button onClick={() => handleDelete(p.id)} className="text-ink-faint hover:text-ink">Excluir</button>
            </div>
          </div>
        ))}
      </EntityTable>

      {data && data.totalPages > 1 && (
        <div className="mt-6 flex justify-center gap-4 text-[13px]">
          <button disabled={page === 0} onClick={() => setPage(page - 1)} className="text-ink-muted disabled:opacity-40">← Anterior</button>
          <span className="text-ink-faint">{page + 1} / {data.totalPages}</span>
          <button disabled={page >= data.totalPages - 1} onClick={() => setPage(page + 1)} className="text-ink-muted disabled:opacity-40">Próxima →</button>
        </div>
      )}
    </div>
  )
}
