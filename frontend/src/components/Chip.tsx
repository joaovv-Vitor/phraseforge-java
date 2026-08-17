import type { ReactNode } from 'react'

export default function Chip({ children }: { children: ReactNode }) {
  return (
    <span className="rounded-[99px] border border-hair px-3 py-0.5 text-xs text-ink-muted">
      {children}
    </span>
  )
}
