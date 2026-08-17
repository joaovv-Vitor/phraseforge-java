import { useCallback, useRef, useState } from 'react'
import { copyToClipboard } from './utils'

export function useCopy() {
  const [copiedId, setCopiedId] = useState<string | null>(null)
  const timer = useRef<number | null>(null)

  const copy = useCallback(async (text: string, id: string) => {
    await copyToClipboard(text)
    setCopiedId(id)
    if (timer.current) window.clearTimeout(timer.current)
    timer.current = window.setTimeout(() => setCopiedId(null), 2000)
  }, [])

  return { copiedId, copy }
}
