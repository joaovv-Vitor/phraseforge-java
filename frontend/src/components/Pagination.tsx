import type { Paged } from '../types/models'

export default function Pagination({
  data,
  onPage,
}: {
  data: Paged<unknown>
  onPage: (page: number) => void
}) {
  if (data.totalPages <= 1) return null
  return (
    <div className="mt-10 flex items-center justify-center gap-4 text-[13px]">
      <button
        disabled={data.page === 0}
        onClick={() => onPage(data.page - 1)}
        className="text-ink-muted transition-colors disabled:opacity-40 hover:text-ink"
      >
        ← Anterior
      </button>
      <span className="text-ink-faint">
        {data.page + 1} / {data.totalPages}
      </span>
      <button
        disabled={data.page >= data.totalPages - 1}
        onClick={() => onPage(data.page + 1)}
        className="text-ink-muted transition-colors disabled:opacity-40 hover:text-ink"
      >
        Próxima →
      </button>
    </div>
  )
}
