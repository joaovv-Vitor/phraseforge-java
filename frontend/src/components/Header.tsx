import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

const navLinks = [
  { to: '/explore', label: 'Explorar' },
  { to: '/autores', label: 'Autores' },
  { to: '/categorias', label: 'Categorias' },
]

export default function Header() {
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()
  const { status, user, logout } = useAuth()

  const signOut = async () => {
    await logout()
    setOpen(false)
    navigate('/')
  }

  return (
    <header className="sticky top-0 z-50 border-b border-hair-subtle bg-paper">
      <div className="mx-auto flex h-[52px] max-w-[1040px] items-center justify-between px-8">
        <button
          onClick={() => navigate('/')}
          className="font-serif text-base font-medium tracking-tight text-ink"
        >
          PhraseForge
        </button>

        <nav className="hidden items-center gap-1 md:flex">
          {navLinks.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `rounded px-3 py-1.5 text-[13px] transition-colors ${
                  isActive ? 'font-medium text-ink' : 'text-ink-muted hover:text-ink'
                }`
              }
            >
              {link.label}
            </NavLink>
          ))}
        </nav>

        <div className="hidden items-center gap-3 md:flex">
          {status === 'authenticated' && user ? (
            <>
              <span className="text-[13px] text-ink-muted">{user.displayName}</span>
              {user.role === 'ADMIN' ? <NavLink to="/admin" className="text-[13px] text-ink-muted transition-colors hover:text-ink">Admin</NavLink> : null}
              <button onClick={signOut} className="text-[13px] text-ink-muted transition-colors hover:text-ink">Sair</button>
            </>
          ) : status === 'anonymous' ? (
            <>
              <NavLink to="/login" className="text-[13px] text-ink-muted transition-colors hover:text-ink">Entrar</NavLink>
              <NavLink to="/cadastro" className="rounded-[99px] border border-hair px-3 py-1.5 text-[13px] text-ink transition-colors hover:border-ink">Criar conta</NavLink>
            </>
          ) : null}
        </div>

        <button className="flex text-ink md:hidden" onClick={() => setOpen((v) => !v)} aria-label="Menu">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
            {open ? <><line x1="3" y1="3" x2="15" y2="15" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" /><line x1="15" y1="3" x2="3" y2="15" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" /></> : <><line x1="3" y1="5" x2="15" y2="5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" /><line x1="3" y1="9" x2="15" y2="9" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" /><line x1="3" y1="13" x2="15" y2="13" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" /></>}
          </svg>
        </button>
      </div>

      {open && (
        <div className="flex flex-col border-t border-hair-subtle px-8 py-4 md:hidden">
          {navLinks.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              onClick={() => setOpen(false)}
              className={({ isActive }) =>
                `border-b border-hair-subtle py-2.5 text-left text-[15px] ${
                  isActive ? 'font-medium text-ink' : 'text-ink-muted'
                }`
              }
            >
              {link.label}
            </NavLink>
          ))}
          {status === 'authenticated' && user ? (
            <>
              {user.role === 'ADMIN' ? <NavLink to="/admin" onClick={() => setOpen(false)} className="border-b border-hair-subtle py-2.5 text-[15px] text-ink-muted">Admin</NavLink> : null}
              <button onClick={signOut} className="py-2.5 text-left text-[15px] text-ink-muted">Sair</button>
            </>
          ) : status === 'anonymous' ? (
            <>
              <NavLink to="/login" onClick={() => setOpen(false)} className="border-b border-hair-subtle py-2.5 text-[15px] text-ink-muted">Entrar</NavLink>
              <NavLink to="/cadastro" onClick={() => setOpen(false)} className="py-2.5 text-[15px] text-ink-muted">Criar conta</NavLink>
            </>
          ) : null}
        </div>
      )}
    </header>
  )
}
