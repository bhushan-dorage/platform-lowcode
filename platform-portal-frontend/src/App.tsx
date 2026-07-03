import { useEffect, useState } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import keycloak from './keycloak'
import SimpleDashboard from './components/Dashboard/SimpleDashboard'
import TaskInbox from './components/TaskInbox/TaskInbox'
import PageView from './pages/PageView'
import PageGenerator from './pages/PageGenerator'

export default function App() {
  const [authenticated, setAuthenticated] = useState<boolean | null>(null)

  useEffect(() => {
    keycloak.init({ onLoad: 'login-required', pkceMethod: 'S256' })
      .then(auth => setAuthenticated(auth))
      .catch(() => setAuthenticated(false))
  }, [])

  if (authenticated === null) return <div>Loading…</div>
  if (!authenticated) return <div>Authentication failed. Please reload.</div>

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<SimpleDashboard />} />
        <Route path="/tasks" element={<TaskInbox />} />
        <Route path="/pages/:pageKey" element={<PageView />} />
        <Route path="/generate" element={<PageGenerator />} />
      </Routes>
    </BrowserRouter>
  )
}
