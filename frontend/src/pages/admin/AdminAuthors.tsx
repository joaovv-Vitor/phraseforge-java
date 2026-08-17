import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuthors, useCreateAuthor, useDeleteAuthor, useUpdateAuthor } from '../../hooks/useAuthors'
import ActionBar from '../../components/admin/ActionBar'
import EntityTable from '../../components/admin/EntityTable'
import AuthorForm from '../../components/admin/AuthorForm'
import Loading from '../../components/Loading'
import type { AuthorPayload, AuthorSummary } from '../../types/models'

export default function AdminAuthors() {
  const { data, isLoading } = useAuthors(0, 100)
  const createAuthor = useCreateAuthor()
  const deleteAuthor = useDeleteAuthor()

  const [editing, setEditing] = useState<AuthorSummary | null>(null)
  const [message, setMessage] = useState('')

  const flash = (msg: string) => {
    setMessage(msg)
    window.setTimeout(() => setMessage(''), 3000)
  }

  const updateAuthor = useUpdateAuthor(editing?.id ?? 0)

  const handleSubmit = async (payload: AuthorPayload) => {
    try {
      if (editing) {
        await updateAuthor.mutateAsync(payload)
        flash('Autor atualizado com sucesso.')
      } else {
        await createAuthor.mutateAsync(payload)
        flash('Autor salvo com sucesso.')
      }
      setEditing(null)
    } catch (err) {
      flash(err instanceof Error ? err.message : 'Erro ao salvar.')
    }
  }

  return (
    <div className="max-w-[900px]">
      <ActionBar
        title="Autores"
        action={
          <button onClick={() => setEditing(null)}
            className="rounded bg-ink px-4 py-2 text-[13px] font-medium text-paper transition-opacity hover:opacity-80">
            + Novo Autor
          </button>
        }
      />

      {message && <div className="mb-6 rounded border border-hair bg-card px-4 py-3 text-sm text-ink-muted">{message}</div>}

      <AuthorForm
        key={editing?.id ?? 'new'}
        author={editing}
        submitLabel={editing ? 'Atualizar Autor' : 'Salvar Autor'}
        onSubmit={handleSubmit}
        onCancel={() => setEditing(null)}
      />

      {isLoading && <Loading />}

      <EntityTable>
        {data?.content.map((a, i) => (
          <div key={a.id}
            className={`flex items-center justify-between gap-4 px-5 py-4 transition-colors hover:bg-card ${i < (data?.content.length ?? 1) - 1 ? 'border-b border-hair-subtle' : ''}`}>
            <div>
              <p className="text-[15px] font-medium text-ink">{a.name}</p>
              <p className="text-[13px] text-ink-faint">{a.phraseCount} {a.phraseCount === 1 ? 'frase' : 'frases'}</p>
            </div>
            <div className="flex shrink-0 gap-3 text-xs">
              <Link to={`/autores/${a.id}`} className="text-ink-muted hover:text-ink">Ver</Link>
              <button onClick={() => setEditing(a)} className="text-ink-muted hover:text-ink">Editar</button>
              <button onClick={() => { if (window.confirm('Excluir este autor?')) deleteAuthor.mutate(a.id) }} className="text-ink-faint hover:text-ink">Excluir</button>
            </div>
          </div>
        ))}
      </EntityTable>
    </div>
  )
}
