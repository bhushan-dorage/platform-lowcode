import { useEffect, useState } from 'react'
import client from '../../../api/client'
import { KpiConfig } from '../../../types/pageSchema'

const ICON_MAP: Record<string, string> = {
  users: '👥',
  tasks: '✅',
  chart: '📈',
  alert: '⚠️',
  check: '✔️',
}

function getNestedValue(obj: unknown, dotPath: string): unknown {
  return dotPath.split('.').reduce((acc: unknown, key) => {
    if (acc !== null && typeof acc === 'object') {
      return (acc as Record<string, unknown>)[key]
    }
    return undefined
  }, obj)
}

function extractValue(responseData: unknown, field: string): unknown {
  if (responseData !== null && typeof responseData === 'object') {
    const data = responseData as Record<string, unknown>
    const tryDataData = getNestedValue(data['data'] && typeof data['data'] === 'object' ? (data['data'] as Record<string, unknown>)['data'] : undefined, field)
    if (tryDataData !== undefined) return tryDataData
    const tryData = getNestedValue(data['data'], field)
    if (tryData !== undefined) return tryData
    const tryDirect = getNestedValue(data, field)
    if (tryDirect !== undefined) return tryDirect
  }
  return undefined
}

interface Props {
  config: KpiConfig
  title?: string
}

export default function KpiWidget({ config, title }: Props) {
  const [value, setValue] = useState<unknown>(null)
  const [unit, setUnit] = useState<string | undefined>(undefined)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  useEffect(() => {
    setLoading(true)
    setError(false)
    client.get(config.dataSource.url)
      .then(r => {
        const val = extractValue(r.data, config.dataSource.valueField)
        setValue(val)
        if (config.dataSource.unitField) {
          const u = extractValue(r.data, config.dataSource.unitField)
          setUnit(u !== undefined ? String(u) : undefined)
        }
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false))
  }, [config.dataSource.url, config.dataSource.valueField, config.dataSource.unitField])

  const iconEmoji = config.icon ? (ICON_MAP[config.icon] ?? '') : ''

  return (
    <div className="kpi-widget">
      {title && <div className="widget-title">{title}</div>}
      <div className="kpi-content">
        {iconEmoji && <span className="kpi-icon" aria-hidden="true">{iconEmoji}</span>}
        <div className="kpi-body">
          <div className="kpi-label">{config.label}</div>
          {loading ? (
            <div className="skeleton skeleton-value" />
          ) : error || value === undefined ? (
            <div className="kpi-value kpi-value--error">-</div>
          ) : (
            <div className="kpi-value">
              {String(value)}{unit ? <span className="kpi-unit"> {unit}</span> : null}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
