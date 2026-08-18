import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { ApiError } from '../services/api'

type Mode = 'login' | 'register'

export default function AuthPage({ mode }: { mode: Mode }) {
  const { login, register } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const registering = mode === 'register'
  const destination = typeof (location.state as { from?: unknown } | null)?.from === 'string'
    ? (location.state as { from: string }).from
    : '/'

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      if (registering) {
        await register({ email, displayName, password })
      } else {
        await login({ email, password })
      }
      navigate(destination, { replace: true })
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Não foi possível concluir a solicitação.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="mx-auto flex min-h-[calc(100vh-52px)] w-full max-w-[440px] items-center px-6 py-14">
      <section className="w-full rounded-sm border border-hair bg-card p-7 shadow-[0_12px_40px_rgb(17_17_16/0.04)] sm:p-9">
        <p className="mb-2 text-[11px] font-semibold uppercase tracking-[0.12em] text-ink-faint">PhraseForge</p>
        <h1 className="mb-2 font-serif text-2xl text-ink">{registering ? 'Crie sua conta' : 'Que bom ter você de volta'}</h1>
        <p className="mb-7 text-sm text-ink-muted">
          {registering ? 'Salve suas frases favoritas e encontre-as quando quiser.' : 'Entre para acessar seus favoritos.'}
        </p>

        <form className="space-y-4" onSubmit={submit}>
          {registering ? (
            <label className="block text-sm font-medium text-ink">
              Nome
              <input
                className="mt-1.5 w-full rounded-sm border border-hair bg-paper px-3 py-2.5 font-normal outline-none transition-colors focus:border-ink"
                value={displayName}
                onChange={(event) => setDisplayName(event.target.value)}
                maxLength={100}
                required
                autoComplete="name"
              />
            </label>
          ) : null}
          <label className="block text-sm font-medium text-ink">
            E-mail
            <input
              className="mt-1.5 w-full rounded-sm border border-hair bg-paper px-3 py-2.5 font-normal outline-none transition-colors focus:border-ink"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              type="email"
              maxLength={254}
              required
              autoComplete="email"
            />
          </label>
          <label className="block text-sm font-medium text-ink">
            Senha
            <input
              className="mt-1.5 w-full rounded-sm border border-hair bg-paper px-3 py-2.5 font-normal outline-none transition-colors focus:border-ink"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              type="password"
              required
              minLength={12}
              autoComplete={registering ? 'new-password' : 'current-password'}
            />
          </label>

          {error ? <p role="alert" className="text-sm text-red-700">{error}</p> : null}

          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-[99px] bg-ink px-5 py-2.5 text-[13px] font-medium text-paper transition-opacity hover:opacity-85 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {submitting ? 'Aguarde…' : registering ? 'Criar conta' : 'Entrar'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-ink-muted">
          {registering ? 'Já tem uma conta?' : 'Ainda não tem uma conta?'}{' '}
          <Link className="font-medium text-ink underline underline-offset-4" to={registering ? '/login' : '/cadastro'}>
            {registering ? 'Entrar' : 'Criar conta'}
          </Link>
        </p>
      </section>
    </main>
  )
}
