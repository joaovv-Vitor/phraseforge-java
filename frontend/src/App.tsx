import { createBrowserRouter } from 'react-router-dom'
import PublicLayout from './layouts/PublicLayout'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <PublicLayout />,
    children: [
      { index: true, element: <div>Home</div> },
      { path: 'explore', element: <div>Explore</div> },
    ],
  },
])
