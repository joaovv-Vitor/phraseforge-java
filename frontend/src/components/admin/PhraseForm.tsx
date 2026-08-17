import { useState } from 'react'
import FormField from './FormField'
import type { AuthorSummary, CategorySummary, Phrase, PhrasePayload, Tag } from '../../types/models'

export default function PhraseForm({
  phrase,
  authors,
  categories,
  tags,
  submitLabel,
  onSubmit,
  onCancel,
}: {
  phrase?: Phrase | null
  authors: AuthorSummary[]
  categories: CategorySummary[]
  tags: Tag[]
  submitLabel: string
  onSubmit: (payload: PhrasePayload) => Promise<void>
  onCancel: () => void
}) {
  const [content, setContent] = useState(phrase?.content ?? '')
  const [authorId, setAuthorId] = useState(phrase ? String(phrase.author.id) : '')
  const [year, setYear] = useState(phrase?.year != null ? String(phrase.year) : '')
  const [language, setLanguage] = useState(phrase?.language ?? 'pt')
  const [source, setSource] = useState(phrase?.source ?? '')
  const [categoryIds, setCategoryIds] = useState<number[]>(
    phrase ? phrase.categories.map((c) => c.id) : [],
  )
  const [tagIds, setTagIds] = useState<number[]>(
    phrase ? phrase.tags.map((t) => t.id) : [],
  )
  const [submitting, setSubmitting] = useState(false)

  const toggle = (list: number[], value: number): number[] =>
    list.includes(value) ? list.filter((v) => v !== value) : [...list, value]

  const submit = async () => {
    setSubmitting(true)
    try {
      await onSubmit({
        content,
        authorId: Number(authorId),
        year: year === '' ? null : Number(year),
        language,
        source: source === '' ? null : source,
        categoryIds,
        tagIds,
      })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mb-10 flex flex-col gap-5 rounded border border-hair bg-card p-6">
      <h3 className="font-serif text-lg text-ink">{phrase ? 'Editar Frase' : 'Nova Frase'}</h3>
      <FormField label="Conteúdo">
        <textarea value={content} onChange={(e) => setContent(e.target.value)} rows={4}
          className="w-full resize-y rounded border border-hair bg-card px-3 py-2.5 font-serif italic leading-[1.6] text-ink outline-none focus:border-ink-muted" />
      </FormField>
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
        <FormField label="Autor">
          <select value={authorId} onChange={(e) => setAuthorId(e.target.value)}
            className="w-full rounded border border-hair bg-card px-3 py-2.5 text-sm text-ink outline-none">
            <option value="">Selecione…</option>
            {authors.map((a) => (
              <option key={a.id} value={a.id}>{a.name}</option>
            ))}
          </select>
        </FormField>
        <FormField label="Idioma">
          <input value={language} onChange={(e) => setLanguage(e.target.value)}
            className="w-full rounded border border-hair bg-card px-3 py-2.5 text-sm text-ink outline-none" />
        </FormField>
        <FormField label="Ano (opcional)">
          <input value={year} onChange={(e) => setYear(e.target.value)} type="number"
            className="w-full rounded border border-hair bg-card px-3 py-2.5 text-sm text-ink outline-none" />
        </FormField>
        <FormField label="Fonte (opcional)">
          <input value={source} onChange={(e) => setSource(e.target.value)}
            className="w-full rounded border border-hair bg-card px-3 py-2.5 text-sm text-ink outline-none" />
        </FormField>
      </div>
      <FormField label="Categorias">
        <div className="flex flex-wrap gap-2">
          {categories.map((c) => (
            <button key={c.id} type="button"
              onClick={() => setCategoryIds((prev) => toggle(prev, c.id))}
              className={`rounded-[99px] border px-3 py-1 text-xs transition-colors ${
                categoryIds.includes(c.id) ? 'border-ink bg-ink text-paper' : 'border-hair text-ink-muted hover:text-ink'
              }`}>
              {c.name}
            </button>
          ))}
        </div>
      </FormField>
      <FormField label="Tags">
        <div className="flex flex-wrap gap-2">
          {tags.map((t) => (
            <button key={t.id} type="button"
              onClick={() => setTagIds((prev) => toggle(prev, t.id))}
              className={`rounded-[99px] border px-3 py-1 text-xs transition-colors ${
                tagIds.includes(t.id) ? 'border-ink bg-ink text-paper' : 'border-hair text-ink-muted hover:text-ink'
              }`}>
              {t.name}
            </button>
          ))}
        </div>
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
