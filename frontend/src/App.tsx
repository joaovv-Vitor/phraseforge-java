import { createBrowserRouter } from 'react-router-dom'
import PublicLayout from './layouts/PublicLayout'
import AdminLayout from './layouts/AdminLayout'
import Home from './pages/Home'
import Explore from './pages/Explore'
import QuoteDetail from './pages/QuoteDetail'
import Authors from './pages/Authors'
import AuthorDetail from './pages/AuthorDetail'
import Categories from './pages/Categories'
import CategoryDetail from './pages/CategoryDetail'
import Dashboard from './pages/admin/Dashboard'
import AdminPhrases from './pages/admin/AdminPhrases'
import AdminAuthors from './pages/admin/AdminAuthors'
import AdminCategories from './pages/admin/AdminCategories'
import AdminTags from './pages/admin/AdminTags'
import AuthPage from './pages/AuthPage'
import Favorites from './pages/Favorites'
import Forbidden from './pages/Forbidden'
import { RequireAuth } from './auth/RequireAuth'
import { RequireRole } from './auth/RequireRole'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <PublicLayout />,
    children: [
      { index: true, element: <Home /> },
      { path: 'explore', element: <Explore /> },
      { path: 'frases/:id', element: <QuoteDetail /> },
      { path: 'autores', element: <Authors /> },
      { path: 'autores/:id', element: <AuthorDetail /> },
      { path: 'categorias', element: <Categories /> },
      { path: 'categorias/:id', element: <CategoryDetail /> },
      { path: 'login', element: <AuthPage mode="login" /> },
      { path: 'cadastro', element: <AuthPage mode="register" /> },
      { path: 'favoritos', element: <RequireAuth><Favorites /></RequireAuth> },
      { path: 'acesso-negado', element: <Forbidden /> },
    ],
  },
  {
    path: '/admin',
    element: <RequireRole role="ADMIN"><AdminLayout /></RequireRole>,
    children: [
      { index: true, element: <Dashboard /> },
      { path: 'frases', element: <AdminPhrases /> },
      { path: 'autores', element: <AdminAuthors /> },
      { path: 'categorias', element: <AdminCategories /> },
      { path: 'tags', element: <AdminTags /> },
    ],
  },
])
