import type { ReactNode } from 'react'

export default function ActionBar({
  title,
  action,
}: {
  title: string
  action?: ReactNode
}) {
  return (
    <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
      <h2 className="font-serif text-[1.75rem] font-normal text-ink">{title}</h2>
      {action}
    </div>
  )
}
