import { useEffect, useState, useCallback } from 'react'
import client from '../../api/client'
import { Task } from '../../types'
import { useWebSocket } from '../../hooks/useWebSocket'

type Filters = {
  status: string
  priority: string
  processKey: string
}

const WS_URL = (import.meta.env.VITE_WS_URL ?? 'ws://localhost:8080') + '/ws/tasks'

export default function TaskInbox() {
  const [tasks, setTasks] = useState<Task[]>([])
  const [loading, setLoading] = useState(false)
  const [cursor, setCursor] = useState<string | undefined>()
  const [hasMore, setHasMore] = useState(true)
  const [filters, setFilters] = useState<Filters>({ status: '', priority: '', processKey: '' })

  const loadTasks = useCallback(async (reset = false) => {
    if (loading) return
    setLoading(true)
    try {
      const params: Record<string, string> = { pageSize: '20' }
      if (!reset && cursor) params.cursor = cursor
      if (filters.status) params.status = filters.status
      if (filters.priority) params.priority = filters.priority
      if (filters.processKey) params.processKey = filters.processKey

      const res = await client.get('/v1/tasks', { params })
      const page = res.data
      setTasks(prev => reset ? page.content : [...prev, ...page.content])
      setCursor(page.cursor)
      setHasMore(page.hasMore ?? false)
    } finally {
      setLoading(false)
    }
  }, [cursor, filters, loading])

  useEffect(() => {
    loadTasks(true)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters])

  useWebSocket(WS_URL, (msg) => {
    if (msg.type === 'TASK_CREATED' || msg.type === 'TASK_CLAIMED') {
      loadTasks(true)
    }
  })

  const priorityColor = (p: Task['priority']) =>
    ({ LOW: 'green', MEDIUM: 'orange', HIGH: 'red', CRITICAL: 'darkred' })[p]

  return (
    <div style={{ padding: 24 }}>
      <h2>Task Inbox</h2>
      <div style={{ display: 'flex', gap: 12, marginBottom: 16 }}>
        <select value={filters.status} onChange={e => setFilters(f => ({ ...f, status: e.target.value }))}>
          <option value="">All Statuses</option>
          <option value="OPEN">Open</option>
          <option value="CLAIMED">Claimed</option>
        </select>
        <select value={filters.priority} onChange={e => setFilters(f => ({ ...f, priority: e.target.value }))}>
          <option value="">All Priorities</option>
          <option value="LOW">Low</option>
          <option value="MEDIUM">Medium</option>
          <option value="HIGH">High</option>
          <option value="CRITICAL">Critical</option>
        </select>
        <input
          placeholder="Process key…"
          value={filters.processKey}
          onChange={e => setFilters(f => ({ ...f, processKey: e.target.value }))}
        />
      </div>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr>
            {['Task Name', 'Process', 'Business Key', 'Priority', 'Due Date', 'Status', 'Actions'].map(h => (
              <th key={h} style={{ textAlign: 'left', padding: '8px 12px', borderBottom: '2px solid #ddd' }}>{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {tasks.map(task => {
            const overdue = task.dueDate && new Date(task.dueDate) < new Date()
            return (
              <tr key={task.id} style={{ borderBottom: '1px solid #eee' }}>
                <td style={{ padding: '8px 12px' }}>{task.name}</td>
                <td style={{ padding: '8px 12px' }}>{task.processKey}</td>
                <td style={{ padding: '8px 12px' }}>{task.businessKey ?? '—'}</td>
                <td style={{ padding: '8px 12px', color: priorityColor(task.priority), fontWeight: 'bold' }}>
                  {task.priority}
                </td>
                <td style={{ padding: '8px 12px', color: overdue ? 'red' : 'inherit' }}>
                  {task.dueDate ? new Date(task.dueDate).toLocaleDateString() : '—'}
                </td>
                <td style={{ padding: '8px 12px' }}>{task.status}</td>
                <td style={{ padding: '8px 12px' }}>
                  <button onClick={() => alert(`Claim task ${task.id}`)}>Claim</button>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
      {hasMore && (
        <button onClick={() => loadTasks()} disabled={loading} style={{ marginTop: 16 }}>
          {loading ? 'Loading…' : 'Load more'}
        </button>
      )}
    </div>
  )
}
