import { Link } from 'react-router-dom'

export default function Brand() {
  return (
    <Link to="/" className="inline-flex items-center gap-2 font-serif text-base font-medium tracking-tight text-ink">
      <img src="/phraseforge-icon.svg" alt="" aria-hidden="true" className="size-[18px]" />
      <span>PhraseForge</span>
    </Link>
  )
}
