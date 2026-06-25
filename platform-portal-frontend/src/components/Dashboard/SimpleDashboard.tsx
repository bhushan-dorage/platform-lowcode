import { useEffect, useState } from 'react'
import client from '../../api/client'

type KpiData = {
  myOpenTasks: number
  overdueTasks: number
  completedToday: number
  slaBreachRate: number
}

type ActivityItem = {
  id: string
  type: string
  description: string
  timestamp: string
}

export default function SimpleDashboard() {
  const [kpi, setKpi] = useState<KpiData>({ myOpenTasks: 0, overdueTasks: 0, completedToday: 0, slaBreachRate: 0 })
  const [activity, setActivity] = useState<ActivityItem[]>([])

  useEffect(() => {
    client.get('/v1/dashboard/kpi').then(r => setKpi(r.data)).catch(() => {})
    client.get('/v1/dashboard/activity').then(r => setActivity(r.data)).catch(() => {})
  }, [])

  return (
    <div style={{ padding: 24 }}>
      <h2>Dashboard</h2>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 32 }}>
        <KpiCard title="My Open Tasks" value={kpi.myOpenTasks} color="#2196F3" />
        <KpiCard title="Overdue Tasks" value={kpi.overdueTasks} color="#f44336" />
        <KpiCard title="Completed Today" value={kpi.completedToday} color="#4CAF50" />
        <KpiCard title="SLA Breach Rate" value={`${(kpi.slaBreachRate * 100).toFixed(1)}%`} color="#FF9800" />
      </div>

      <h3>Recent Activity</h3>
      <div>
        {activity.length === 0 && <p style={{ color: '#999' }}>No recent activity</p>}
        {activity.map(item => (
          <div key={item.id} style={{
            padding: '12px 16px', marginBottom: 8,
            border: '1px solid #eee', borderRadius: 4
          }}>
            <span style={{ fontWeight: 'bold', marginRight: 8 }}>{item.type}</span>
            <span>{item.description}</span>
            <span style={{ float: 'right', color: '#999', fontSize: 12 }}>
              {new Date(item.timestamp).toLocaleString()}
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}

function KpiCard({ title, value, color }: { title: string; value: number | string; color: string }) {
  return (
    <div style={{
      background: '#fff', border: '1px solid #eee', borderRadius: 8,
      padding: 20, textAlign: 'center', boxShadow: '0 2px 4px rgba(0,0,0,.06)'
    }}>
      <div style={{ fontSize: 32, fontWeight: 'bold', color }}>{value}</div>
      <div style={{ color: '#666', marginTop: 8 }}>{title}</div>
    </div>
  )
}
