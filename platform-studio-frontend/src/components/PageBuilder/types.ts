export type WidgetType = 'kpi' | 'table' | 'chart' | 'form' | 'text';

export interface KpiConfig {
  label: string;
  dataSource: { url: string; valueField: string; unitField?: string };
  icon?: 'users' | 'tasks' | 'chart' | 'alert' | 'check';
  trend?: boolean;
}

export interface TableColumnDef {
  field: string;
  header: string;
  type?: 'text' | 'date' | 'badge';
}

export interface TableConfig {
  dataSource: { url: string; pageSize?: number };
  columns: TableColumnDef[];
  searchable?: boolean;
}

export interface ChartConfig {
  chartType: 'bar' | 'line' | 'pie';
  dataSource: { url: string; labelField: string; valueField: string };
}

export interface FormWidgetConfig {
  formKey: string;
  submitUrl?: string;
  successMessage?: string;
}

export interface TextConfig {
  content: string;
  variant?: 'default' | 'info' | 'warning' | 'success';
}

export type WidgetConfig = KpiConfig | TableConfig | ChartConfig | FormWidgetConfig | TextConfig;

export interface BuiltWidget {
  id: string;
  type: WidgetType;
  title: string;
  colSpan: number;
  config: WidgetConfig;
}

export interface BuiltSection {
  id: string;
  title: string;
  columns: 1 | 2 | 3 | 4;
  widgets: BuiltWidget[];
}

export const WIDGET_DRAG_TYPE = 'PAGE_WIDGET';

export interface PaletteItem {
  type: WidgetType;
  label: string;
  description: string;
  icon: string;
}

export const PALETTE_WIDGETS: PaletteItem[] = [
  { type: 'kpi',   label: 'KPI Card',      description: 'Single metric with label & trend', icon: '▣' },
  { type: 'table', label: 'Data Table',     description: 'Rows and columns from a data source', icon: '⊟' },
  { type: 'chart', label: 'Chart',          description: 'Bar, line, or pie chart', icon: '▲' },
  { type: 'form',  label: 'Embedded Form',  description: 'Form from the Form Service', icon: '☰' },
  { type: 'text',  label: 'Text Block',     description: 'Announcement or guidance note', icon: 'T' },
];

export function defaultConfig(type: WidgetType): WidgetConfig {
  switch (type) {
    case 'kpi':
      return {
        label: 'My Metric',
        dataSource: { url: '/api/v1/metrics/count', valueField: 'value' },
        icon: 'chart',
        trend: false,
      } satisfies KpiConfig;
    case 'table':
      return {
        dataSource: { url: '/api/v1/entities/record', pageSize: 10 },
        columns: [
          { field: 'id', header: 'ID', type: 'text' },
          { field: 'name', header: 'Name', type: 'text' },
          { field: 'status', header: 'Status', type: 'badge' },
        ],
        searchable: true,
      } satisfies TableConfig;
    case 'chart':
      return {
        chartType: 'bar',
        dataSource: { url: '/api/v1/metrics/series', labelField: 'label', valueField: 'value' },
      } satisfies ChartConfig;
    case 'form':
      return {
        formKey: 'my-form',
        submitUrl: '/api/v1/entities/record',
        successMessage: 'Submitted successfully.',
      } satisfies FormWidgetConfig;
    case 'text':
      return {
        content: 'Enter your message here.',
        variant: 'default',
      } satisfies TextConfig;
  }
}

export function schemaFromSections(
  pageName: string,
  sections: BuiltSection[],
): string {
  return JSON.stringify({
    version: '1.0',
    title: pageName,
    layout: {
      type: 'sections',
      sections: sections.map((s) => ({
        id: s.id,
        title: s.title || undefined,
        columns: s.columns,
        widgets: s.widgets.map((w) => ({
          id: w.id,
          type: w.type,
          title: w.title || undefined,
          colSpan: w.colSpan > 1 ? w.colSpan : undefined,
          config: w.config,
        })),
      })),
    },
  });
}

let _counter = 0;
export function uid(prefix: string) {
  return `${prefix}_${Date.now()}_${++_counter}`;
}
