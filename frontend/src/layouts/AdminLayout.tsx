import { Link, NavLink, Outlet } from 'react-router-dom'

const navItems = [
  { to: '/admin', label: 'Painel', end: true },
  { to: '/admin/frases', label: 'Frases' },
  { to: '/admin/autores', label: 'Autores' },
  { to: '/admin/categorias', label: 'Categorias' },
  { to: '/admin/tags', label: 'Tags' },
]

export default function AdminLayout() {
  return (
    <div className="min-h-screen bg-paper">
      <div className="flex h-[52px] items-center justify-between border-b border-hair-subtle bg-paper px-8">
        <Link to="/" className="font-serif text-base font-medium tracking-tight text-ink">
          PhraseForge
        </Link>
        <span className="text-[11px] font-semibold uppercase tracking-[0.1em] text-ink-faint">Admin</span>
        <Link to="/" className="text-[13px] text-ink-muted transition-colors hover:text-ink">
          ← Voltar ao site
        </Link>
      </div>

      <div className="flex">
        <aside className="hidden w-[200px] shrink-0 border-r border-hair-subtle p-4 pt-8 md:block">
          <p className="mb-4 px-2 text-[11px] font-semibold uppercase tracking-[0.1em] text-ink-faint">
            Admin
          </p>
          <nav className="flex flex-col gap-0.5">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  `rounded px-2 py-1.5 text-left text-sm transition-colors ${
                    isActive ? 'bg-hair-subtle font-medium text-ink' : 'text-ink-muted hover:text-ink'
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
        </aside>

        <main className="min-w-0 flex-1 p-8 md:p-10">
          <Outlet />
        </main>
      </div>

      <div className="border-t border-hair-subtle p-4 md:hidden">
        <nav className="flex flex-wrap gap-2">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className="rounded border border-hair px-3 py-1 text-[13px] text-ink-muted"
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </div>
    </div>
  )
}
