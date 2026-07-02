export type WidgetType = 'kpi' | 'table' | 'form' | 'text' | 'chart'

export interface KpiConfig {
  label: string
  dataSource: { url: string; valueField: string; unitField?: string }
  icon?: 'users' | 'tasks' | 'chart' | 'alert' | 'check'
  trend?: boolean
}

export interface TableConfig {
  dataSource: { url: string; pageSize?: number }
  columns: { field: string; header: string; type?: 'text' | 'date' | 'badge' }[]
  searchable?: boolean
}

export interface FormConfig {
  formKey: string
  submitUrl?: string
  successMessage?: string
}

export interface TextConfig {
  content: string
  variant?: 'info' | 'warning' | 'success' | 'default'
}

export interface ChartConfig {
  chartType: 'bar' | 'line' | 'pie'
  dataSource: { url: string; labelField: string; valueField: string }
}

export type WidgetConfig = KpiConfig | TableConfig | FormConfig | TextConfig | ChartConfig

export interface PageWidget {
  id: string
  type: WidgetType
  title?: string
  colSpan?: number
  config: WidgetConfig
}

export interface PageSection {
  id: string
  title?: string
  columns: 1 | 2 | 3 | 4
  widgets: PageWidget[]
}

export interface PageSchema {
  version: '1.0'
  title: string
  description?: string
  layout: {
    type: 'sections'
    sections: PageSection[]
  }
}

export interface PageDefinitionDto {
  id: string
  pageKey: string
  name: string
  description?: string
  schema: string  // JSON string of PageSchema
  status: 'DRAFT' | 'PUBLISHED' | 'DEPRECATED'
  version: number
  createdAt: string
  updatedAt: string
}
