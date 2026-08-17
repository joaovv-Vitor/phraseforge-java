import { useState } from 'react'
import { useTags, useCreateTag, useDeleteTag, useUpdateTag } from '../../hooks/useTags'
import ActionBar from '../../components/admin/ActionBar'
import TagForm from '../../components/admin/TagForm'
import Loading from '../../components/Loading'
import type { Tag, TagPayload } from '../../types/models'

export default function AdminTags() {
  const { data, isLoading } = useTags(0, 100)
  const createTag = useCreateTag()
  const deleteTag = useDeleteTag()

  const [editing, setEditing] = useState<Tag | null>(null)
  const [message, setMessage] = useState('')

  const flash = (msg: string) => {
    setMessage(msg)
    window.setTimeout(() => setMessage(''), 3000)
  }

  const updateTag = useUpdateTag(editing?.id ?? 0)

  const handleSubmit = async (payload: TagPayload) => {
    try {
      if (editing) {
        await updateTag.mutateAsync(payload)
        flash('Tag atualizada com sucesso.')
      } else {
        await createTag.mutateAsync(payload)
        flash('Tag salva com sucesso.')
      }
      setEditing(null)
    } catch (err) {
      flash(err instanceof Error ? err.message : 'Erro ao salvar.')
    }
  }

  return (
    <div className="max-w-[900px]">
      <ActionBar
        title="Tags"
        action={
          <button onClick={() => setEditing(null)}
            className="rounded bg-ink px-4 py-2 text-[13px] font-medium text-paper transition-opacity hover:opacity-80">
            + Nova Tag
          </button>
        }
      />

      {message && <div className="mb-6 rounded border border-hair bg-card px-4 py-3 text-sm text-ink-muted">{message}</div>}

      <TagForm
        key={editing?.id ?? 'new'}
        tag={editing}
        submitLabel={editing ? 'Atualizar Tag' : 'Salvar Tag'}
        onSubmit={handleSubmit}
        onCancel={() => setEditing(null)}
      />

      {isLoading && <Loading />}

      <div className="flex flex-wrap gap-2">
        {data?.content.map((t) => (
          <div key={t.id} className="flex items-center gap-2 rounded-[99px] border border-hair px-3 py-1.5">
            <button onClick={() => setEditing(t)} className="text-[13px] text-ink-muted hover:text-ink">{t.name}</button>
            <button onClick={() => { if (window.confirm('Excluir esta tag?')) deleteTag.mutate(t.id) }}
              className="text-xs text-ink-faint hover:text-ink">×</button>
          </div>
        ))}
      </div>
    </div>
  )
}
