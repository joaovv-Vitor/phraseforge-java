import { useState } from 'react'
import FormField from './FormField'
import type { AuthorPayload, AuthorSummary } from '../../types/models'

export default function AuthorForm({
  author,
  submitLabel,
  onSubmit,
  onCancel,
}: {
  author?: AuthorSummary | null
  submitLabel: string
  onSubmit: (payload: AuthorPayload) => Promise<void>
  onCancel: () => void
}) {
  const [name, setName] = useState(author?.name ?? '')
  const [birthYear, setBirthYear] = useState(author?.birthYear != null ? String(author.birthYear) : '')
  const [deathYear, setDeathYear] = useState(author?.deathYear != null ? String(author.deathYear) : '')
  const [biography, setBiography] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const submit = async () => {
    setSubmitting(true)
    try {
      await onSubmit({
        name,
        birthYear: birthYear === '' ? null : Number(birthYear),
        deathYear: deathYear === '' ? null : Number(deathYear),
        biography: biography === '' ? null : biography,
      })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mb-10 flex flex-col gap-5 rounded border border-hair bg-card p-6">
      <h3 className="font-serif text-lg text-ink">{author ? 'Editar Autor' : 'Novo Autor'}</h3>
      <FormField label="Nome">
        <input value={name} onChange={(e) => setName(e.target.value)}
          className="w-full rounded border border-hair bg-card px-3 py-2.5 text-sm text-ink outline-none focus:border-ink-muted" />
      </FormField>
      <div className="grid grid-cols-2 gap-5">
        <FormField label="Nascimento (opcional)">
          <input value={birthYear} onChange={(e) => setBirthYear(e.target.value)} type="number"
            className="w-full rounded border border-hair bg-card px-3 py-2.5 text-sm text-ink outline-none" />
        </FormField>
        <FormField label="Falecimento (opcional)">
          <input value={deathYear} onChange={(e) => setDeathYear(e.target.value)} type="number"
            className="w-full rounded border border-hair bg-card px-3 py-2.5 text-sm text-ink outline-none" />
        </FormField>
      </div>
      <FormField label="Biografia (opcional)">
        <textarea value={biography} onChange={(e) => setBiography(e.target.value)} rows={4}
          className="w-full resize-y rounded border border-hair bg-card px-3 py-2.5 text-sm text-ink outline-none" />
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
