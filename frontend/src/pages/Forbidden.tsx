import { Link } from 'react-router-dom'

export default function Forbidden() {
  return (
    <main className="mx-auto flex min-h-[calc(100vh-52px)] max-w-[640px] flex-col items-center justify-center px-8 text-center">
      <p className="mb-3 text-[11px] font-semibold uppercase tracking-[0.1em] text-ink-faint">Acesso restrito</p>
      <h1 className="mb-3 font-serif text-3xl text-ink">Você não tem permissão para esta área.</h1>
      <p className="mb-7 text-sm text-ink-muted">A administração está disponível apenas para contas com papel de administrador.</p>
      <Link to="/" className="rounded-[99px] border border-hair px-5 py-2 text-[13px] font-medium text-ink transition-colors hover:border-ink">Voltar ao início</Link>
    </main>
  )
}
