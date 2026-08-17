export default function StatCard({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="rounded border border-hair bg-card p-6">
      <p className="mb-2 text-xs text-ink-faint">{label}</p>
      <p className="font-serif text-3xl font-normal text-ink">{value}</p>
    </div>
  )
}
