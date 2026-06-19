import React from 'react';
import { FieldDefinition } from './types';

interface Props {
  field: FieldDefinition | null;
  onChange: (updated: FieldDefinition) => void;
}

const inputStyle: React.CSSProperties = {
  width: '100%', padding: '6px 8px',
  border: '1px solid var(--border)', borderRadius: 6,
  fontSize: 13, marginTop: 4, marginBottom: 12,
};

const labelStyle: React.CSSProperties = {
  fontSize: 11, fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase',
};

export default function FieldEditor({ field, onChange }: Props) {
  if (!field) {
    return (
      <div style={{ width: 220, padding: 16, borderLeft: '1px solid var(--border)', color: 'var(--text-muted)', fontSize: 13 }}>
        Select a field to edit its properties.
      </div>
    );
  }

  const update = (patch: Partial<FieldDefinition>) => onChange({ ...field, ...patch });

  return (
    <div style={{ width: 220, padding: 16, borderLeft: '1px solid var(--border)', overflowY: 'auto', background: 'var(--surface)' }}>
      <div style={{ fontWeight: 600, marginBottom: 16 }}>Field Properties</div>

      <div style={labelStyle}>Label</div>
      <input style={inputStyle} value={field.label} onChange={(e) => update({ label: e.target.value })} />

      <div style={labelStyle}>Field Name</div>
      <input style={inputStyle} value={field.name} onChange={(e) => update({ name: e.target.value })} />

      {(field.type === 'text' || field.type === 'email' || field.type === 'number' || field.type === 'textarea') && (
        <>
          <div style={labelStyle}>Placeholder</div>
          <input style={inputStyle} value={field.placeholder ?? ''} onChange={(e) => update({ placeholder: e.target.value })} />
        </>
      )}

      {field.type === 'textarea' && (
        <>
          <div style={labelStyle}>Rows</div>
          <input style={inputStyle} type="number" value={field.rows ?? 3} onChange={(e) => update({ rows: Number(e.target.value) })} />
        </>
      )}

      {field.type === 'select' && (
        <>
          <div style={labelStyle}>Options (one per line)</div>
          <textarea
            style={{ ...inputStyle, height: 80, resize: 'vertical' }}
            value={(field.options ?? []).join('\n')}
            onChange={(e) => update({ options: e.target.value.split('\n').filter(Boolean) })}
          />
        </>
      )}

      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4 }}>
        <input
          type="checkbox"
          checked={field.required ?? false}
          onChange={(e) => update({ required: e.target.checked })}
          id="req-check"
        />
        <label htmlFor="req-check" style={{ fontSize: 13 }}>Required</label>
      </div>
    </div>
  );
}
