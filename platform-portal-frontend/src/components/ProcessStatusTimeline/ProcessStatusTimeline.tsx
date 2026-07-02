import { useEffect, useState } from 'react'
import client from '../../api/client'
import { ProcessHistoryEntry } from '../../types'

const ICON: Record<string, string> = {
  startEvent: '▶',
  endEvent: '⏹',
  userTask: '👤',
  serviceTask: '⚙',
  exclusiveGateway: '◆',
  default: '○',
}

type Props = { processId: string }

export default function ProcessStatusTimeline({ processId }: Props) {
  const [history, setHistory] = useState<ProcessHistoryEntry[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    client.get(`/v1/processes/${processId}/history`)
      .then(r => setHistory(r.data))
      .finally(() => setLoading(false))
  }, [processId])

  if (loading) return <div>Loading history…</div>

  return (
    <div style={{ padding: 16 }}>
      <h3>Process Timeline</h3>
      <div style={{ position: 'relative', paddingLeft: 32 }}>
        <div style={{
          position: 'absolute', left: 14, top: 0, bottom: 0,
          width: 2, background: '#ddd'
        }} />
        {history.map((entry, i) => (
          <div key={entry.id ?? i} style={{ marginBottom: 20, position: 'relative' }}>
            <div style={{
              position: 'absolute', left: -24, top: 2,
              width: 20, height: 20, borderRadius: '50%',
              background: entry.endTime ? '#4CAF50' : '#2196F3',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 11, color: '#fff'
            }}>
              {ICON[entry.activityType] ?? ICON.default}
            </div>
            <div style={{ fontWeight: 'bold' }}>{entry.activityName ?? entry.activityId}</div>
            <div style={{ fontSize: 12, color: '#666' }}>
              {new Date(entry.startTime).toLocaleString()}
              {entry.endTime && ` → ${new Date(entry.endTime).toLocaleString()}`}
              {entry.durationMs != null && ` (${(entry.durationMs / 1000).toFixed(1)}s)`}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
