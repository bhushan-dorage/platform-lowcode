import { useEffect, useState } from 'react'
import client from '../../../api/client'
import { ChartConfig } from '../../../types/pageSchema'

const MAX_ITEMS = 10
const BAR_MAX_HEIGHT = 120
const BAR_WIDTH = 32
const BAR_GAP = 8
const LABEL_HEIGHT = 24

function getNestedValue(obj: unknown, dotPath: string): unknown {
  return dotPath.split('.').reduce((acc: unknown, key) => {
    if (acc !== null && typeof acc === 'object') {
      return (acc as Record<string, unknown>)[key]
    }
    return undefined
  }, obj)
}

interface ChartItem {
  label: string
  value: number
}

interface Props {
  config: ChartConfig
  title?: string
}

export default function ChartWidget({ config, title }: Props) {
  const [items, setItems] = useState<ChartItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  useEffect(() => {
    setLoading(true)
    setError(false)
    client.get(config.dataSource.url)
      .then(r => {
        let arr: unknown[] = []
        const data = r.data
        if (Array.isArray(data)) {
          arr = data
        } else if (data && typeof data === 'object') {
          const d = data as Record<string, unknown>
          if (Array.isArray(d['data'])) {
            arr = d['data'] as unknown[]
          } else if (d['data'] && typeof d['data'] === 'object') {
            const nested = d['data'] as Record<string, unknown>
            if (Array.isArray(nested['data'])) {
              arr = nested['data'] as unknown[]
            }
          }
        }
        const parsed: ChartItem[] = arr.slice(0, MAX_ITEMS).map(item => ({
          label: String(getNestedValue(item, config.dataSource.labelField) ?? ''),
          value: Number(getNestedValue(item, config.dataSource.valueField) ?? 0),
        }))
        setItems(parsed)
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false))
  }, [config.dataSource.url, config.dataSource.labelField, config.dataSource.valueField])

  if (loading) {
    return (
      <div className="chart-widget">
        {title && <div className="widget-title">{title}</div>}
        <div className="skeleton skeleton-chart" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="chart-widget">
        {title && <div className="widget-title">{title}</div>}
        <div className="widget-error">Failed to load chart data.</div>
      </div>
    )
  }

  if (items.length === 0) {
    return (
      <div className="chart-widget">
        {title && <div className="widget-title">{title}</div>}
        <div className="widget-empty">No data available.</div>
      </div>
    )
  }

  const maxValue = Math.max(...items.map(i => i.value), 1)
  const svgWidth = items.length * (BAR_WIDTH + BAR_GAP) + BAR_GAP
  const svgHeight = BAR_MAX_HEIGHT + LABEL_HEIGHT

  return (
    <div className="chart-widget">
      {title && <div className="widget-title">{title}</div>}
      <div className="chart-container" style={{ overflowX: 'auto' }}>
        <svg
          width={svgWidth}
          height={svgHeight}
          role="img"
          aria-label={title ?? 'Bar chart'}
          style={{ display: 'block', minWidth: '100%' }}
        >
          {items.map((item, i) => {
            const barHeight = Math.max(4, Math.round((item.value / maxValue) * BAR_MAX_HEIGHT))
            const x = BAR_GAP + i * (BAR_WIDTH + BAR_GAP)
            const y = BAR_MAX_HEIGHT - barHeight
            return (
              <g key={i}>
                <title>{item.label}: {item.value}</title>
                <rect
                  x={x}
                  y={y}
                  width={BAR_WIDTH}
                  height={barHeight}
                  fill="#3b82f6"
                  rx={3}
                />
                <text
                  x={x + BAR_WIDTH / 2}
                  y={BAR_MAX_HEIGHT + LABEL_HEIGHT - 4}
                  textAnchor="middle"
                  fontSize="10"
                  fill="#6b7280"
                >
                  {item.label.length > 6 ? item.label.slice(0, 5) + '…' : item.label}
                </text>
              </g>
            )
          })}
        </svg>
      </div>
    </div>
  )
}
