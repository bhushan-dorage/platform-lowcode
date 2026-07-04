import { useDrag } from 'react-dnd';
import { FIELD_DRAG_TYPE, FieldType, PALETTE_FIELDS } from './types';

interface DragItem { type: typeof FIELD_DRAG_TYPE; fieldType: FieldType }

function PaletteItem({ fieldType, label, icon }: { fieldType: FieldType; label: string; icon: string }) {
  const [{ isDragging }, drag] = useDrag<DragItem, void, { isDragging: boolean }>({
    type: FIELD_DRAG_TYPE,
    item: { type: FIELD_DRAG_TYPE, fieldType },
    collect: (monitor) => ({ isDragging: monitor.isDragging() }),
  });

  return (
    <div
      ref={drag}
      style={{
        display: 'flex', alignItems: 'center', gap: 8,
        padding: '8px 10px', marginBottom: 4,
        border: '1px solid var(--border)', borderRadius: 6,
        background: isDragging ? '#e0e7ff' : 'var(--surface)',
        cursor: 'grab', userSelect: 'none',
        opacity: isDragging ? 0.5 : 1,
        fontSize: 13,
      }}
    >
      <span style={{ width: 20, textAlign: 'center', fontWeight: 700, color: 'var(--primary)' }}>{icon}</span>
      <span>{label}</span>
    </div>
  );
}

export default function FieldPalette() {
  return (
    <div style={{
      width: 180, flexShrink: 0,
      background: '#f9fafb', borderRight: '1px solid var(--border)',
      padding: 12, overflowY: 'auto',
    }}>
      <div style={{ fontSize: 11, fontWeight: 600, color: 'var(--text-muted)', marginBottom: 10, textTransform: 'uppercase' }}>
        Components
      </div>
      {PALETTE_FIELDS.map((f) => (
        <PaletteItem key={f.type} fieldType={f.type} label={f.label} icon={f.icon} />
      ))}
    </div>
  );
}
