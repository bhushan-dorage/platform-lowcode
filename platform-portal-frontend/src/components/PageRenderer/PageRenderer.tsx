import { PageSchema, PageSection, PageWidget, KpiConfig, TableConfig, FormConfig, TextConfig, ChartConfig } from '../../types/pageSchema'
import KpiWidget from './widgets/KpiWidget'
import TableWidget from './widgets/TableWidget'
import TextWidget from './widgets/TextWidget'
import FormWidget from './widgets/FormWidget'
import ChartWidget from './widgets/ChartWidget'
import './PageRenderer.css'

function renderWidget(widget: PageWidget) {
  switch (widget.type) {
    case 'kpi':
      return <KpiWidget config={widget.config as KpiConfig} title={widget.title} />
    case 'table':
      return <TableWidget config={widget.config as TableConfig} title={widget.title} />
    case 'form':
      return <FormWidget config={widget.config as FormConfig} title={widget.title} />
    case 'text':
      return <TextWidget config={widget.config as TextConfig} title={widget.title} />
    case 'chart':
      return <ChartWidget config={widget.config as ChartConfig} title={widget.title} />
    default:
      return <div className="widget-error">Unknown widget type.</div>
  }
}

function SectionGrid({ section }: { section: PageSection }) {
  return (
    <div className="page-section">
      {section.title && (
        <div className="page-section__title">{section.title}</div>
      )}
      <div
        className="page-section__grid"
        style={{ '--cols': section.columns } as React.CSSProperties}
      >
        {section.widgets.map(widget => (
          <div
            key={widget.id}
            className="widget-card"
            style={
              widget.colSpan && widget.colSpan > 1
                ? { gridColumn: `span ${Math.min(widget.colSpan, section.columns)}` }
                : undefined
            }
          >
            {renderWidget(widget)}
          </div>
        ))}
      </div>
    </div>
  )
}

export default function PageRenderer({ schema }: { schema: PageSchema }) {
  return (
    <div className="page-renderer">
      {schema.layout.sections.map(section => (
        <SectionGrid key={section.id} section={section} />
      ))}
    </div>
  )
}
