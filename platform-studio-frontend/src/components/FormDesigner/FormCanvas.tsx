import React from 'react';
import { useDrop } from 'react-dnd';
import { FIELD_DRAG_TYPE, FieldDefinition, FieldType } from './types';

interface DropItem { type: typeof FIELD_DRAG_TYPE; fieldType: FieldType }

function FieldCard({
  field, selected, onSelect, onDelete, onMoveUp, onMoveDown,
}: {
  field: FieldDefinition;
  selected: boolean;
  onSelect: () => void;
  onDelete: () => void;
  onMoveUp: () => void;
  onMoveDown: () => void;
}) {
  return (
    <div
      onClick={onSelect}
      style={{
        padding: '10px 14px', marginBottom: 8,
        border: `2px solid ${selected ? 'var(--primary)' : 'var(--border)'}`,
        borderRadius: 8, background: 'var(--surface)',
        cursor: 'pointer', position: 'relative',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <span style={{ fontSize: 11, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
            {field.type}
          </span>
          <div style={{ fontWeight: 500, marginTop: 2 }}>{field.label}</div>
          {field.required && <span style={{ fontSize: 10, color: '#dc2626' }}>Required</span>}
        </div>
        <div style={{ display: 'flex', gap: 4 }}>
          <button className="btn-ghost" style={{ padding: '2px 6px', fontSize: 11 }} onClick={(e) => { e.stopPropagation(); onMoveUp(); }}>↑</button>
          <button className="btn-ghost" style={{ padding: '2px 6px', fontSize: 11 }} onClick={(e) => { e.stopPropagation(); onMoveDown(); }}>↓</button>
          <button
            style={{ background: '#fee2e2', color: '#dc2626', border: 'none', borderRadius: 4, padding: '2px 6px', fontSize: 11, cursor: 'pointer' }}
            onClick={(e) => { e.stopPropagation(); onDelete(); }}
          >✕</button>
        </div>
      </div>
    </div>
  );
}

interface Props {
  fields: FieldDefinition[];
  selectedId: string | null;
  onDrop: (fieldType: FieldType) => void;
  onSelect: (id: string) => void;
  onDelete: (id: string) => void;
  onMove: (id: string, direction: 'up' | 'down') => void;
}

export default function FormCanvas({ fields, selectedId, onDrop, onSelect, onDelete, onMove }: Props) {
  const [{ isOver }, drop] = useDrop<DropItem, void, { isOver: boolean }>({
    accept: FIELD_DRAG_TYPE,
    drop: (item) => onDrop(item.fieldType),
    collect: (monitor) => ({ isOver: monitor.isOver() }),
  });

  return (
    <div
      ref={drop}
      style={{
        flex: 1, padding: 20, overflowY: 'auto',
        background: isOver ? '#eef2ff' : '#f9fafb',
        transition: 'background 0.15s',
        minHeight: 400,
      }}
    >
      {fields.length === 0 && (
        <div style={{
          textAlign: 'center', color: 'var(--text-muted)', padding: '60px 20px',
          border: '2px dashed var(--border)', borderRadius: 12,
        }}>
          <div style={{ fontSize: 32, marginBottom: 8 }}>📋</div>
          <div>Drag components here to build your form</div>
        </div>
      )}
      {fields.map((field) => (
        <FieldCard
          key={field.id}
          field={field}
          selected={field.id === selectedId}
          onSelect={() => onSelect(field.id)}
          onDelete={() => onDelete(field.id)}
          onMoveUp={() => onMove(field.id, 'up')}
          onMoveDown={() => onMove(field.id, 'down')}
        />
      ))}
    </div>
  );
}
