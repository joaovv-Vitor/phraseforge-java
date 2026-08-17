import { Link } from 'react-router-dom'

export default function Footer() {
  return (
    <footer className="border-t border-hair-subtle py-6 text-center">
      <Link to="/admin" className="text-xs text-ink-faint transition-colors hover:text-ink-muted">
        Admin
      </Link>
    </footer>
  )
}
