import type { ReactNode } from 'react'

export default function FormField({
  label,
  children,
}: {
  label: string
  children: ReactNode
}) {
  return (
    <div>
      <label className="mb-1.5 block text-[13px] font-medium text-ink-muted">{label}</label>
      {children}
    </div>
  )
}
