import { TextConfig } from '../../../types/pageSchema'

const VARIANT_STYLES: Record<string, React.CSSProperties> = {
  info: { backgroundColor: '#eff6ff', borderLeft: '4px solid #3b82f6', color: '#1e40af' },
  warning: { backgroundColor: '#fffbeb', borderLeft: '4px solid #f59e0b', color: '#92400e' },
  success: { backgroundColor: '#f0fdf4', borderLeft: '4px solid #22c55e', color: '#166534' },
  default: {},
}

interface Props {
  config: TextConfig
  title?: string
}

export default function TextWidget({ config, title }: Props) {
  const variant = config.variant ?? 'default'
  const style = VARIANT_STYLES[variant] ?? {}

  return (
    <div className="text-widget">
      {title && <div className="widget-title">{title}</div>}
      <div className="text-content" style={style}>
        {config.content}
      </div>
    </div>
  )
}
