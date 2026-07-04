import React from 'react';
import {
  BuiltWidget, KpiConfig, TableConfig, ChartConfig,
  FormWidgetConfig, TextConfig, TableColumnDef,
} from './types';

interface Props {
  widget: BuiltWidget | null;
  maxColSpan: number;
  onChange: (updated: BuiltWidget) => void;
}

const inputS: React.CSSProperties = {
  width: '100%', padding: '5px 8px',
  border: '1px solid var(--border)', borderRadius: 5,
  fontSize: 12, marginTop: 3, marginBottom: 10,
  background: 'var(--surface)', color: 'var(--text)',
};
const labelS: React.CSSProperties = {
  fontSize: 10, fontWeight: 700, color: 'var(--text-muted)',
  textTransform: 'uppercase', letterSpacing: '0.06em', display: 'block',
};
const sectionS: React.CSSProperties = {
  borderTop: '1px solid var(--border)', paddingTop: 10, marginTop: 2,
};
const subheadS: React.CSSProperties = {
  fontSize: 11, fontWeight: 700, color: 'var(--text)',
  textTransform: 'uppercase', letterSpacing: '0.05em',
  marginBottom: 8,
};

/* ── KPI editor ─────────────────────────────────── */
function KpiEditor({ cfg, onChange }: { cfg: KpiConfig; onChange: (c: KpiConfig) => void }) {
  return (
    <>
      <label style={labelS}>Metric Label</label>
      <input style={inputS} value={cfg.label} onChange={(e) => onChange({ ...cfg, label: e.target.value })} />

      <label style={labelS}>Data Source URL</label>
      <input style={inputS} value={cfg.dataSource.url}
        onChange={(e) => onChange({ ...cfg, dataSource: { ...cfg.dataSource, url: e.target.value } })} />

      <label style={labelS}>Value Field</label>
      <input style={inputS} value={cfg.dataSource.valueField}
        onChange={(e) => onChange({ ...cfg, dataSource: { ...cfg.dataSource, valueField: e.target.value } })} />

      <label style={labelS}>Unit Field (optional)</label>
      <input style={inputS} value={cfg.dataSource.unitField ?? ''}
        onChange={(e) => onChange({ ...cfg, dataSource: { ...cfg.dataSource, unitField: e.target.value || undefined } })} />

      <label style={labelS}>Icon</label>
      <select style={inputS} value={cfg.icon ?? 'chart'}
        onChange={(e) => onChange({ ...cfg, icon: e.target.value as KpiConfig['icon'] })}>
        <option value="chart">Chart</option>
        <option value="users">Users</option>
        <option value="tasks">Tasks</option>
        <option value="alert">Alert</option>
        <option value="check">Check</option>
      </select>

      <label style={{ ...labelS, display: 'flex', alignItems: 'center', gap: 6, marginBottom: 10 }}>
        <input type="checkbox" checked={!!cfg.trend}
          onChange={(e) => onChange({ ...cfg, trend: e.target.checked })} />
        Show trend indicator
      </label>
    </>
  );
}

/* ── Table editor ───────────────────────────────── */
function TableEditor({ cfg, onChange }: { cfg: TableConfig; onChange: (c: TableConfig) => void }) {
  const updateCol = (idx: number, patch: Partial<TableColumnDef>) => {
    const cols = cfg.columns.map((c, i) => i === idx ? { ...c, ...patch } : c);
    onChange({ ...cfg, columns: cols });
  };
  const addCol = () => onChange({ ...cfg, columns: [...cfg.columns, { field: '', header: 'New Column', type: 'text' }] });
  const removeCol = (idx: number) => onChange({ ...cfg, columns: cfg.columns.filter((_, i) => i !== idx) });

  return (
    <>
      <label style={labelS}>Data Source URL</label>
      <input style={inputS} value={cfg.dataSource.url}
        onChange={(e) => onChange({ ...cfg, dataSource: { ...cfg.dataSource, url: e.target.value } })} />

      <label style={labelS}>Page Size</label>
      <input style={inputS} type="number" min={1} max={100}
        value={cfg.dataSource.pageSize ?? 10}
        onChange={(e) => onChange({ ...cfg, dataSource: { ...cfg.dataSource, pageSize: Number(e.target.value) } })} />

      <label style={{ ...labelS, display: 'flex', alignItems: 'center', gap: 6, marginBottom: 10 }}>
        <input type="checkbox" checked={!!cfg.searchable}
          onChange={(e) => onChange({ ...cfg, searchable: e.target.checked })} />
        Enable search bar
      </label>

      <div style={sectionS}>
        <div style={subheadS}>Columns</div>
        {cfg.columns.map((col, idx) => (
          <div key={idx} style={{ background: '#f9fafb', border: '1px solid var(--border)', borderRadius: 6, padding: '8px', marginBottom: 6 }}>
            <label style={labelS}>Header</label>
            <input style={inputS} value={col.header} onChange={(e) => updateCol(idx, { header: e.target.value })} />
            <label style={labelS}>Field Name</label>
            <input style={inputS} value={col.field} onChange={(e) => updateCol(idx, { field: e.target.value })} />
            <label style={labelS}>Display Type</label>
            <select style={inputS} value={col.type ?? 'text'} onChange={(e) => updateCol(idx, { type: e.target.value as TableColumnDef['type'] })}>
              <option value="text">Text</option>
              <option value="date">Date</option>
              <option value="badge">Badge</option>
            </select>
            <button onClick={() => removeCol(idx)}
              style={{ fontSize: 11, color: '#dc2626', background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}>
              Remove column
            </button>
          </div>
        ))}
        <button onClick={addCol}
          style={{ fontSize: 12, color: 'var(--primary)', background: 'none', border: 'none', cursor: 'pointer', padding: '4px 0' }}>
          + Add column
        </button>
      </div>
    </>
  );
}

/* ── Chart editor ───────────────────────────────── */
function ChartEditor({ cfg, onChange }: { cfg: ChartConfig; onChange: (c: ChartConfig) => void }) {
  return (
    <>
      <label style={labelS}>Chart Type</label>
      <select style={inputS} value={cfg.chartType}
        onChange={(e) => onChange({ ...cfg, chartType: e.target.value as ChartConfig['chartType'] })}>
        <option value="bar">Bar</option>
        <option value="line">Line</option>
        <option value="pie">Pie</option>
      </select>

      <label style={labelS}>Data Source URL</label>
      <input style={inputS} value={cfg.dataSource.url}
        onChange={(e) => onChange({ ...cfg, dataSource: { ...cfg.dataSource, url: e.target.value } })} />

      <label style={labelS}>Label Field</label>
      <input style={inputS} value={cfg.dataSource.labelField}
        onChange={(e) => onChange({ ...cfg, dataSource: { ...cfg.dataSource, labelField: e.target.value } })} />

      <label style={labelS}>Value Field</label>
      <input style={inputS} value={cfg.dataSource.valueField}
        onChange={(e) => onChange({ ...cfg, dataSource: { ...cfg.dataSource, valueField: e.target.value } })} />
    </>
  );
}

/* ── Form editor ────────────────────────────────── */
function FormEditor({ cfg, onChange }: { cfg: FormWidgetConfig; onChange: (c: FormWidgetConfig) => void }) {
  return (
    <>
      <label style={labelS}>Form Key</label>
      <input style={inputS} value={cfg.formKey}
        onChange={(e) => onChange({ ...cfg, formKey: e.target.value })} />

      <label style={labelS}>Submit URL</label>
      <input style={inputS} value={cfg.submitUrl ?? ''}
        onChange={(e) => onChange({ ...cfg, submitUrl: e.target.value || undefined })} />

      <label style={labelS}>Success Message</label>
      <input style={inputS} value={cfg.successMessage ?? ''}
        onChange={(e) => onChange({ ...cfg, successMessage: e.target.value || undefined })} />
    </>
  );
}

/* ── Text editor ────────────────────────────────── */
function TextEditor({ cfg, onChange }: { cfg: TextConfig; onChange: (c: TextConfig) => void }) {
  return (
    <>
      <label style={labelS}>Variant</label>
      <select style={inputS} value={cfg.variant ?? 'default'}
        onChange={(e) => onChange({ ...cfg, variant: e.target.value as TextConfig['variant'] })}>
        <option value="default">Default</option>
        <option value="info">Info (teal)</option>
        <option value="warning">Warning (amber)</option>
        <option value="success">Success (green)</option>
      </select>

      <label style={labelS}>Content</label>
      <textarea
        style={{ ...inputS, height: 100, resize: 'vertical', fontFamily: 'inherit' }}
        value={cfg.content}
        onChange={(e) => onChange({ ...cfg, content: e.target.value })}
      />
    </>
  );
}

/* ── Widget Editor (right panel) ────────────────── */
export default function WidgetEditor({ widget, maxColSpan, onChange }: Props) {
  if (!widget) {
    return (
      <div style={{
        width: 240, flexShrink: 0,
        padding: '16px 14px',
        borderLeft: '1px solid var(--border)',
        color: 'var(--text-muted)',
        fontSize: 12,
        lineHeight: 1.6,
        background: 'var(--surface)',
      }}>
        <div style={{ fontWeight: 600, color: 'var(--text)', marginBottom: 8 }}>Widget Properties</div>
        Click any widget on the canvas to edit its properties here.
      </div>
    );
  }

  const update = (patch: Partial<BuiltWidget>) => onChange({ ...widget, ...patch });

  return (
    <div style={{
      width: 240, flexShrink: 0,
      borderLeft: '1px solid var(--border)',
      padding: '14px 14px',
      overflowY: 'auto',
      background: 'var(--surface)',
    }}>
      <div style={{ fontWeight: 700, fontSize: 13, marginBottom: 14 }}>
        Widget Properties
      </div>

      {/* Common fields */}
      <label style={labelS}>Title</label>
      <input style={inputS} value={widget.title}
        onChange={(e) => update({ title: e.target.value })}
        placeholder="Widget title (optional)" />

      <label style={labelS}>Column Span (1–{maxColSpan})</label>
      <select style={inputS} value={widget.colSpan}
        onChange={(e) => update({ colSpan: Number(e.target.value) })}>
        {Array.from({ length: maxColSpan }, (_, i) => i + 1).map((n) => (
          <option key={n} value={n}>{n} {n === 1 ? 'column' : 'columns'}</option>
        ))}
      </select>

      {/* Type-specific config */}
      <div style={sectionS}>
        <div style={subheadS}>{widget.type} settings</div>

        {widget.type === 'kpi' && (
          <KpiEditor
            cfg={widget.config as KpiConfig}
            onChange={(c) => update({ config: c })}
          />
        )}
        {widget.type === 'table' && (
          <TableEditor
            cfg={widget.config as TableConfig}
            onChange={(c) => update({ config: c })}
          />
        )}
        {widget.type === 'chart' && (
          <ChartEditor
            cfg={widget.config as ChartConfig}
            onChange={(c) => update({ config: c })}
          />
        )}
        {widget.type === 'form' && (
          <FormEditor
            cfg={widget.config as FormWidgetConfig}
            onChange={(c) => update({ config: c })}
          />
        )}
        {widget.type === 'text' && (
          <TextEditor
            cfg={widget.config as TextConfig}
            onChange={(c) => update({ config: c })}
          />
        )}
      </div>
    </div>
  );
}
