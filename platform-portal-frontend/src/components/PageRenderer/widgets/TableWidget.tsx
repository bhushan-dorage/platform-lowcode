import { useEffect, useState } from 'react'
import client from '../../../api/client'
import { TableConfig } from '../../../types/pageSchema'

function getNestedValue(obj: unknown, dotPath: string): unknown {
  return dotPath.split('.').reduce((acc: unknown, key) => {
    if (acc !== null && typeof acc === 'object') {
      return (acc as Record<string, unknown>)[key]
    }
    return undefined
  }, obj)
}

function formatCell(value: unknown, type?: 'text' | 'date' | 'badge'): React.ReactNode {
  if (value === null || value === undefined) return '-'
  if (type === 'date') {
    const d = new Date(String(value))
    return isNaN(d.getTime()) ? String(value) : d.toLocaleDateString()
  }
  if (type === 'badge') {
    return <span className="badge">{String(value)}</span>
  }
  return String(value)
}

interface Props {
  config: TableConfig
  title?: string
}

export default function TableWidget({ config, title }: Props) {
  const [rows, setRows] = useState<unknown[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const pageSize = config.dataSource.pageSize ?? 10

  useEffect(() => {
    setLoading(true)
    setError(false)
    client.get(config.dataSource.url)
      .then(r => {
        const data = r.data
        let items: unknown[] = []
        if (Array.isArray(data)) {
          items = data
        } else if (data && typeof data === 'object') {
          const d = data as Record<string, unknown>
          if (Array.isArray(d['data'])) {
            items = d['data'] as unknown[]
          } else if (d['data'] && typeof d['data'] === 'object') {
            const nested = d['data'] as Record<string, unknown>
            if (Array.isArray(nested['data'])) {
              items = nested['data'] as unknown[]
            } else if (Array.isArray(nested['content'])) {
              items = nested['content'] as unknown[]
            }
          } else if (Array.isArray(d['content'])) {
            items = d['content'] as unknown[]
          }
        }
        setRows(items.slice(0, pageSize))
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false))
  }, [config.dataSource.url, pageSize])

  return (
    <div className="table-widget">
      {title && <div className="widget-title">{title}</div>}
      {loading ? (
        <div className="skeleton-table">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="skeleton skeleton-row" />
          ))}
        </div>
      ) : error ? (
        <div className="widget-error">Failed to load data.</div>
      ) : rows.length === 0 ? (
        <div className="widget-empty">No data available.</div>
      ) : (
        <div className="table-scroll">
          <table className="widget-table">
            <thead>
              <tr>
                {config.columns.map(col => (
                  <th key={col.field}>{col.header}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.map((row, i) => (
                <tr key={i}>
                  {config.columns.map(col => (
                    <td key={col.field}>
                      {formatCell(getNestedValue(row, col.field), col.type)}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
