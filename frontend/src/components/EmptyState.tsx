export default function EmptyState({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <div className="py-16 text-center text-ink-faint">
      <p className="mb-1 font-serif text-lg text-ink">{title}</p>
      {subtitle && <p className="text-sm">{subtitle}</p>}
    </div>
  )
}
