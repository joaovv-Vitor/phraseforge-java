import type { ReactNode } from 'react'

export default function PillButton({
  children,
  onClick,
  active = false,
}: {
  children: ReactNode
  onClick?: () => void
  active?: boolean
}) {
  return (
    <button
      onClick={onClick}
      className={`rounded-[99px] border px-3.5 py-1.5 text-[13px] transition-all ${
        active
          ? 'border-ink bg-ink font-medium text-paper'
          : 'border-hair text-ink-muted hover:border-ink-muted hover:text-ink'
      }`}
    >
      {children}
    </button>
  )
}
