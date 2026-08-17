import { createBrowserRouter } from 'react-router-dom'
import PublicLayout from './layouts/PublicLayout'
import Home from './pages/Home'
import Explore from './pages/Explore'
import QuoteDetail from './pages/QuoteDetail'
import Authors from './pages/Authors'
import AuthorDetail from './pages/AuthorDetail'
import Categories from './pages/Categories'
import CategoryDetail from './pages/CategoryDetail'

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
    ],
  },
])
