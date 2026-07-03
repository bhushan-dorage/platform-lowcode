import React from 'react';
import { useDrag } from 'react-dnd';
import { WIDGET_DRAG_TYPE, WidgetType, PALETTE_WIDGETS } from './types';

interface DragItem { dragType: typeof WIDGET_DRAG_TYPE; widgetType: WidgetType }

function PaletteCard({ type, label, description, icon }: {
  type: WidgetType; label: string; description: string; icon: string;
}) {
  const [{ isDragging }, drag] = useDrag<DragItem, void, { isDragging: boolean }>({
    type: WIDGET_DRAG_TYPE,
    item: { dragType: WIDGET_DRAG_TYPE, widgetType: type },
    collect: (m) => ({ isDragging: m.isDragging() }),
  });

  return (
    <div
      ref={drag}
      title={`Drag to add a ${label}`}
      style={{
        display: 'flex',
        alignItems: 'flex-start',
        gap: 10,
        padding: '9px 10px',
        marginBottom: 6,
        border: '1px solid var(--border)',
        borderRadius: 7,
        background: isDragging ? '#eef2ff' : 'var(--surface)',
        cursor: 'grab',
        userSelect: 'none',
        opacity: isDragging ? 0.45 : 1,
        transition: 'background 0.1s, opacity 0.1s',
      }}
    >
      <div style={{
        width: 28, height: 28, flexShrink: 0,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        background: '#ede9fe', borderRadius: 6,
        fontWeight: 700, fontSize: 13, color: '#6d28d9',
      }}>
        {icon}
      </div>
      <div>
        <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text)', lineHeight: 1.3 }}>{label}</div>
        <div style={{ fontSize: 11, color: 'var(--text-muted)', lineHeight: 1.4, marginTop: 1 }}>{description}</div>
      </div>
    </div>
  );
}

export default function WidgetPalette() {
  return (
    <div style={{
      width: 200,
      flexShrink: 0,
      background: '#f9fafb',
      borderRight: '1px solid var(--border)',
      padding: '14px 12px',
      overflowY: 'auto',
      display: 'flex',
      flexDirection: 'column',
    }}>
      <div style={{
        fontSize: 11, fontWeight: 600, color: 'var(--text-muted)',
        textTransform: 'uppercase', letterSpacing: '0.06em',
        marginBottom: 10,
      }}>
        Widgets
      </div>
      <div style={{ fontSize: 11, color: 'var(--text-muted)', marginBottom: 12, lineHeight: 1.5 }}>
        Drag a widget onto a section below.
      </div>
      {PALETTE_WIDGETS.map((w) => (
        <PaletteCard key={w.type} type={w.type} label={w.label} description={w.description} icon={w.icon} />
      ))}
    </div>
  );
}
