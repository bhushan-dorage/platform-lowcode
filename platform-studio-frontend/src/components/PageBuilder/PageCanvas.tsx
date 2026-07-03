import React from 'react';
import { useDrop } from 'react-dnd';
import { WIDGET_DRAG_TYPE, BuiltSection, BuiltWidget, WidgetType, uid, defaultConfig } from './types';

interface DragItem { dragType: typeof WIDGET_DRAG_TYPE; widgetType: WidgetType }

const WIDGET_ICONS: Record<WidgetType, string> = {
  kpi: '▣', table: '⊟', chart: '▲', form: '☰', text: 'T',
};

const WIDGET_COLORS: Record<WidgetType, string> = {
  kpi: '#dbeafe', table: '#d1fae5', chart: '#ede9fe', form: '#fef3c7', text: '#f3f4f6',
};
const WIDGET_TEXT_COLORS: Record<WidgetType, string> = {
  kpi: '#1d4ed8', table: '#065f46', chart: '#6d28d9', form: '#92400e', text: '#374151',
};

/* ── Widget card inside canvas ─────────────────── */
function WidgetCard({
  widget, selected, colCount,
  onSelect, onDelete, onMoveLeft, onMoveRight,
}: {
  widget: BuiltWidget;
  selected: boolean;
  colCount: number;
  onSelect: () => void;
  onDelete: () => void;
  onMoveLeft: () => void;
  onMoveRight: () => void;
}) {
  return (
    <div
      onClick={onSelect}
      style={{
        border: `2px solid ${selected ? 'var(--primary)' : 'var(--border)'}`,
        borderRadius: 8,
        background: 'var(--surface)',
        padding: '10px 12px',
        cursor: 'pointer',
        position: 'relative',
        gridColumn: widget.colSpan > 1 ? `span ${Math.min(widget.colSpan, colCount)}` : undefined,
        boxShadow: selected ? '0 0 0 3px rgba(99,102,241,0.15)' : undefined,
        transition: 'border-color 0.12s',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
        <span style={{
          display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
          width: 22, height: 22, borderRadius: 5,
          background: WIDGET_COLORS[widget.type],
          color: WIDGET_TEXT_COLORS[widget.type],
          fontSize: 11, fontWeight: 700,
        }}>
          {WIDGET_ICONS[widget.type]}
        </span>
        <span style={{ fontSize: 11, fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          {widget.type}
        </span>
        {widget.colSpan > 1 && (
          <span style={{ fontSize: 10, color: 'var(--text-muted)' }}>span {widget.colSpan}</span>
        )}
      </div>
      <div style={{ fontWeight: 500, fontSize: 13, color: 'var(--text)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
        {widget.title || <em style={{ color: 'var(--text-muted)', fontWeight: 400 }}>Untitled</em>}
      </div>
      <div style={{ display: 'flex', gap: 3, marginTop: 8 }}>
        <button className="btn-ghost" style={{ padding: '1px 5px', fontSize: 10 }}
          onClick={(e) => { e.stopPropagation(); onMoveLeft(); }}>←</button>
        <button className="btn-ghost" style={{ padding: '1px 5px', fontSize: 10 }}
          onClick={(e) => { e.stopPropagation(); onMoveRight(); }}>→</button>
        <button
          style={{ marginLeft: 'auto', background: '#fee2e2', color: '#dc2626', border: 'none', borderRadius: 4, padding: '1px 5px', fontSize: 10, cursor: 'pointer' }}
          onClick={(e) => { e.stopPropagation(); onDelete(); }}
        >✕</button>
      </div>
    </div>
  );
}

/* ── Section ───────────────────────────────────── */
function SectionBlock({
  section, selectedWidgetId,
  onDropWidget, onSelectWidget, onDeleteWidget, onMoveWidget,
  onChangeTitle, onChangeColumns, onDeleteSection,
}: {
  section: BuiltSection;
  selectedWidgetId: string | null;
  onDropWidget: (sectionId: string, widgetType: WidgetType) => void;
  onSelectWidget: (widgetId: string) => void;
  onDeleteWidget: (sectionId: string, widgetId: string) => void;
  onMoveWidget: (sectionId: string, widgetId: string, dir: 'left' | 'right') => void;
  onChangeTitle: (sectionId: string, title: string) => void;
  onChangeColumns: (sectionId: string, cols: 1 | 2 | 3 | 4) => void;
  onDeleteSection: (sectionId: string) => void;
}) {
  const [{ isOver }, drop] = useDrop<DragItem, void, { isOver: boolean }>({
    accept: WIDGET_DRAG_TYPE,
    drop: (item) => onDropWidget(section.id, item.widgetType),
    collect: (m) => ({ isOver: m.isOver() }),
  });

  return (
    <div
      style={{
        border: '1px solid var(--border)',
        borderRadius: 10,
        marginBottom: 16,
        background: 'var(--surface)',
        overflow: 'hidden',
      }}
    >
      {/* Section header */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: 10,
        padding: '8px 14px',
        background: '#f1f5f9',
        borderBottom: '1px solid var(--border)',
      }}>
        <input
          value={section.title}
          onChange={(e) => onChangeTitle(section.id, e.target.value)}
          placeholder="Section title (optional)"
          style={{
            flex: 1, border: 'none', background: 'transparent',
            fontSize: 13, fontWeight: 600, color: 'var(--text)',
            outline: 'none', minWidth: 0,
          }}
        />
        {/* Column picker */}
        <div style={{ display: 'flex', gap: 3, flexShrink: 0 }}>
          {([1, 2, 3, 4] as const).map((n) => (
            <button
              key={n}
              onClick={() => onChangeColumns(section.id, n)}
              style={{
                width: 24, height: 24,
                borderRadius: 4,
                border: '1px solid var(--border)',
                background: section.columns === n ? 'var(--primary)' : 'var(--surface)',
                color: section.columns === n ? '#fff' : 'var(--text-muted)',
                fontSize: 11, fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              {n}
            </button>
          ))}
        </div>
        <span style={{ fontSize: 10, color: 'var(--text-muted)', flexShrink: 0 }}>cols</span>
        <button
          onClick={() => onDeleteSection(section.id)}
          style={{
            background: 'none', border: 'none', cursor: 'pointer',
            color: '#9ca3af', fontSize: 14, padding: '0 2px', flexShrink: 0,
          }}
          title="Delete section"
        >✕</button>
      </div>

      {/* Drop zone + widget grid */}
      <div
        ref={drop}
        style={{
          padding: 14,
          minHeight: 80,
          background: isOver ? '#eef2ff' : 'transparent',
          transition: 'background 0.12s',
          display: 'grid',
          gridTemplateColumns: `repeat(${section.columns}, 1fr)`,
          gap: 10,
          alignItems: 'start',
        }}
      >
        {section.widgets.length === 0 && (
          <div style={{
            gridColumn: `1 / -1`,
            textAlign: 'center', color: 'var(--text-muted)',
            fontSize: 12, padding: '18px 0',
            border: `2px dashed ${isOver ? '#818cf8' : 'var(--border)'}`,
            borderRadius: 8,
            transition: 'border-color 0.12s',
          }}>
            {isOver ? 'Release to add widget' : 'Drag a widget here'}
          </div>
        )}
        {section.widgets.map((w) => (
          <WidgetCard
            key={w.id}
            widget={w}
            colCount={section.columns}
            selected={w.id === selectedWidgetId}
            onSelect={() => onSelectWidget(w.id)}
            onDelete={() => onDeleteWidget(section.id, w.id)}
            onMoveLeft={() => onMoveWidget(section.id, w.id, 'left')}
            onMoveRight={() => onMoveWidget(section.id, w.id, 'right')}
          />
        ))}
        {section.widgets.length > 0 && (
          <div
            style={{
              gridColumn: `1 / -1`,
              textAlign: 'center', color: 'var(--text-muted)',
              fontSize: 11, padding: '8px',
              border: `2px dashed ${isOver ? '#818cf8' : 'var(--border)'}`,
              borderRadius: 6,
              transition: 'border-color 0.12s',
            }}
          >
            {isOver ? 'Release to add' : '+ drag another widget here'}
          </div>
        )}
      </div>
    </div>
  );
}

/* ── Page Canvas ───────────────────────────────── */
interface Props {
  sections: BuiltSection[];
  selectedWidgetId: string | null;
  onAddSection: () => void;
  onDropWidget: (sectionId: string, widgetType: WidgetType) => void;
  onSelectWidget: (widgetId: string) => void;
  onDeleteWidget: (sectionId: string, widgetId: string) => void;
  onMoveWidget: (sectionId: string, widgetId: string, dir: 'left' | 'right') => void;
  onChangeSectionTitle: (sectionId: string, title: string) => void;
  onChangeSectionColumns: (sectionId: string, cols: 1 | 2 | 3 | 4) => void;
  onDeleteSection: (sectionId: string) => void;
}

export default function PageCanvas({
  sections, selectedWidgetId,
  onAddSection, onDropWidget, onSelectWidget,
  onDeleteWidget, onMoveWidget,
  onChangeSectionTitle, onChangeSectionColumns, onDeleteSection,
}: Props) {
  return (
    <div style={{ flex: 1, overflowY: 'auto', padding: '16px 20px', background: '#f8fafc' }}>
      {sections.length === 0 && (
        <div style={{
          textAlign: 'center', color: 'var(--text-muted)',
          padding: '60px 20px',
          border: '2px dashed var(--border)', borderRadius: 12,
          marginBottom: 16,
        }}>
          <div style={{ fontSize: 28, marginBottom: 10 }}>⊞</div>
          <div style={{ fontWeight: 600, marginBottom: 4 }}>Start by adding a section</div>
          <div style={{ fontSize: 12 }}>Sections organise your page into rows. Widgets live inside sections.</div>
        </div>
      )}

      {sections.map((s) => (
        <SectionBlock
          key={s.id}
          section={s}
          selectedWidgetId={selectedWidgetId}
          onDropWidget={onDropWidget}
          onSelectWidget={onSelectWidget}
          onDeleteWidget={onDeleteWidget}
          onMoveWidget={onMoveWidget}
          onChangeTitle={onChangeSectionTitle}
          onChangeColumns={onChangeSectionColumns}
          onDeleteSection={onDeleteSection}
        />
      ))}

      <button
        onClick={onAddSection}
        style={{
          width: '100%',
          padding: '9px 0',
          border: '2px dashed var(--border)',
          borderRadius: 8,
          background: 'transparent',
          color: 'var(--text-muted)',
          fontSize: 13,
          cursor: 'pointer',
          fontWeight: 500,
          transition: 'border-color 0.12s, color 0.12s',
        }}
        onMouseEnter={(e) => {
          (e.currentTarget as HTMLButtonElement).style.borderColor = 'var(--primary)';
          (e.currentTarget as HTMLButtonElement).style.color = 'var(--primary)';
        }}
        onMouseLeave={(e) => {
          (e.currentTarget as HTMLButtonElement).style.borderColor = 'var(--border)';
          (e.currentTarget as HTMLButtonElement).style.color = 'var(--text-muted)';
        }}
      >
        + Add Section
      </button>
    </div>
  );
}

export { uid, defaultConfig };
