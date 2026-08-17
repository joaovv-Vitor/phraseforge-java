import type { ReactNode } from 'react'

export default function EntityTable({ children }: { children: ReactNode }) {
  return (
    <div className="overflow-hidden rounded border border-hair">
      <div className="flex flex-col">{children}</div>
    </div>
  )
}
