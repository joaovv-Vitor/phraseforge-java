export async function copyToClipboard(text: string): Promise<void> {
  await navigator.clipboard.writeText(text)
}

export function formatYear(year: number | null): string {
  return year === null ? '' : String(year)
}
