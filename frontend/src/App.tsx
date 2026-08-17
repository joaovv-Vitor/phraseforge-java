import { createBrowserRouter } from 'react-router-dom'
import PublicLayout from './layouts/PublicLayout'
import Home from './pages/Home'
import Explore from './pages/Explore'
import QuoteDetail from './pages/QuoteDetail'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <PublicLayout />,
    children: [
      { index: true, element: <Home /> },
      { path: 'explore', element: <Explore /> },
      { path: 'frases/:id', element: <QuoteDetail /> },
    ],
  },
])
