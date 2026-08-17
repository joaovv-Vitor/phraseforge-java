import { useState } from 'react'
import FormField from './FormField'
import type { Tag, TagPayload } from '../../types/models'

export default function TagForm({
  tag,
  submitLabel,
  onSubmit,
  onCancel,
}: {
  tag?: Tag | null
  submitLabel: string
  onSubmit: (payload: TagPayload) => Promise<void>
  onCancel: () => void
}) {
  const [name, setName] = useState(tag?.name ?? '')
  const [submitting, setSubmitting] = useState(false)

  const submit = async () => {
    setSubmitting(true)
    try {
      await onSubmit({ name })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mb-10 flex flex-col gap-5 rounded border border-hair bg-card p-6">
      <h3 className="font-serif text-lg text-ink">{tag ? 'Editar Tag' : 'Nova Tag'}</h3>
      <FormField label="Nome">
        <input value={name} onChange={(e) => setName(e.target.value)}
          className="w-full rounded border border-hair bg-card px-3 py-2.5 text-sm text-ink outline-none focus:border-ink-muted" />
      </FormField>
      <div className="flex gap-3">
        <button onClick={submit} disabled={submitting}
          className="rounded bg-ink px-6 py-2.5 text-sm font-medium text-paper transition-opacity hover:opacity-80 disabled:opacity-50">
          {submitLabel}
        </button>
        {onCancel && (
          <button onClick={onCancel}
            className="rounded border border-hair px-6 py-2.5 text-sm text-ink-muted hover:text-ink">
            Cancelar
          </button>
        )}
      </div>
    </div>
  )
}
