import { useCallback, useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { DndProvider } from 'react-dnd';
import { HTML5Backend } from 'react-dnd-html5-backend';

import { BuiltSection, BuiltWidget, WidgetType, uid, defaultConfig, schemaFromSections } from './types';
import WidgetPalette from './WidgetPalette';
import PageCanvas from './PageCanvas';
import WidgetEditor from './WidgetEditor';
import { pageBuilderApi } from '../../api/pageBuilderApi';

function slugify(s: string) {
  return s.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'my-page';
}

type SaveState = 'idle' | 'saving' | 'saved' | 'error';

export default function PageBuilderPage() {
  const { id: pageKeyParam } = useParams<{ id?: string }>();
  const navigate = useNavigate();

  const [pageName, setPageName]     = useState('My New Page');
  const [pageKey, setPageKey]       = useState('my-new-page');
  const [keyEdited, setKeyEdited]   = useState(false);
  const [sections, setSections]     = useState<BuiltSection[]>([]);
  const [selectedWidgetId, setSelectedWidgetId] = useState<string | null>(null);
  const [saveState, setSaveState]   = useState<SaveState>('idle');
  const [saveMsg, setSaveMsg]       = useState('');
  const [isNew, setIsNew]           = useState(true);

  // Load existing page if editing
  useEffect(() => {
    if (!pageKeyParam) return;
    setIsNew(false);
    pageBuilderApi.get(pageKeyParam).then((rec) => {
      setPageName(rec.name);
      setPageKey(rec.pageKey);
      setKeyEdited(true);
      try {
        const schema = JSON.parse(rec.schema);
        const loaded: BuiltSection[] = (schema.layout?.sections ?? []).map((s: BuiltSection) => ({
          id: s.id ?? uid('sec'),
          title: s.title ?? '',
          columns: s.columns ?? 2,
          widgets: (s.widgets ?? []).map((w: BuiltWidget) => ({
            id: w.id ?? uid('w'),
            type: w.type,
            title: w.title ?? '',
            colSpan: w.colSpan ?? 1,
            config: w.config ?? defaultConfig(w.type),
          })),
        }));
        setSections(loaded);
      } catch {
        /* malformed schema — start fresh */
      }
    }).catch(() => {/* page not found — start fresh */});
  }, [pageKeyParam]);

  // Auto-slug pageName → pageKey when user hasn't manually edited the key
  useEffect(() => {
    if (!keyEdited) setPageKey(slugify(pageName));
  }, [pageName, keyEdited]);

  /* ── Section mutations ─────────────────────── */
  const addSection = useCallback(() => {
    setSections((prev) => [...prev, {
      id: uid('sec'), title: '', columns: 2, widgets: [],
    }]);
  }, []);

  const deleteSection = useCallback((sectionId: string) => {
    setSections((prev) => prev.filter((s) => s.id !== sectionId));
    setSelectedWidgetId(null);
  }, []);

  const changeSectionTitle = useCallback((sectionId: string, title: string) => {
    setSections((prev) => prev.map((s) => s.id === sectionId ? { ...s, title } : s));
  }, []);

  const changeSectionColumns = useCallback((sectionId: string, columns: 1 | 2 | 3 | 4) => {
    setSections((prev) => prev.map((s) => s.id === sectionId ? { ...s, columns } : s));
  }, []);

  /* ── Widget mutations ──────────────────────── */
  const dropWidget = useCallback((sectionId: string, widgetType: WidgetType) => {
    const w: BuiltWidget = {
      id: uid('w'),
      type: widgetType,
      title: '',
      colSpan: 1,
      config: defaultConfig(widgetType),
    };
    setSections((prev) => prev.map((s) =>
      s.id === sectionId ? { ...s, widgets: [...s.widgets, w] } : s,
    ));
    setSelectedWidgetId(w.id);
  }, []);

  const deleteWidget = useCallback((sectionId: string, widgetId: string) => {
    setSections((prev) => prev.map((s) =>
      s.id === sectionId
        ? { ...s, widgets: s.widgets.filter((w) => w.id !== widgetId) }
        : s,
    ));
    setSelectedWidgetId((prev) => prev === widgetId ? null : prev);
  }, []);

  const moveWidget = useCallback((sectionId: string, widgetId: string, dir: 'left' | 'right') => {
    setSections((prev) => prev.map((s) => {
      if (s.id !== sectionId) return s;
      const idx = s.widgets.findIndex((w) => w.id === widgetId);
      if (idx < 0) return s;
      const next = [...s.widgets];
      const target = dir === 'left' ? idx - 1 : idx + 1;
      if (target < 0 || target >= next.length) return s;
      [next[idx], next[target]] = [next[target], next[idx]];
      return { ...s, widgets: next };
    }));
  }, []);

  const updateWidget = useCallback((updated: BuiltWidget) => {
    setSections((prev) => prev.map((s) => ({
      ...s,
      widgets: s.widgets.map((w) => w.id === updated.id ? updated : w),
    })));
  }, []);

  /* ── Derived: selected widget + its section's column count ── */
  let selectedWidget: BuiltWidget | null = null;
  let selectedSectionColumns = 4;
  for (const s of sections) {
    const found = s.widgets.find((w) => w.id === selectedWidgetId);
    if (found) { selectedWidget = found; selectedSectionColumns = s.columns; break; }
  }

  /* ── Save ──────────────────────────────────── */
  const handleSave = async () => {
    setSaveState('saving');
    setSaveMsg('');
    const schema = schemaFromSections(pageName, sections);
    try {
      if (isNew) {
        await pageBuilderApi.create({ pageKey, name: pageName, schema });
        setIsNew(false);
        navigate(`/pages/${pageKey}`, { replace: true });
      } else {
        await pageBuilderApi.update(pageKeyParam ?? pageKey, { name: pageName, schema });
      }
      setSaveState('saved');
      setSaveMsg('Saved');
      setTimeout(() => setSaveState('idle'), 2500);
    } catch (err: unknown) {
      setSaveState('error');
      setSaveMsg(err instanceof Error ? err.message : 'Save failed');
    }
  };

  const handlePublish = async () => {
    setSaveState('saving');
    const schema = schemaFromSections(pageName, sections);
    try {
      if (isNew) await pageBuilderApi.create({ pageKey, name: pageName, schema });
      await pageBuilderApi.publish(pageKeyParam ?? pageKey, schema);
      setSaveState('saved');
      setSaveMsg('Published');
      setTimeout(() => setSaveState('idle'), 2500);
    } catch (err: unknown) {
      setSaveState('error');
      setSaveMsg(err instanceof Error ? err.message : 'Publish failed');
    }
  };

  /* ── Render ────────────────────────────────── */
  const isBusy = saveState === 'saving';

  return (
    <DndProvider backend={HTML5Backend}>
      <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
        {/* Top bar */}
        <div style={{
          display: 'flex', alignItems: 'center', gap: 10,
          padding: '10px 16px',
          borderBottom: '1px solid var(--border)',
          background: 'var(--surface)',
          flexShrink: 0,
          flexWrap: 'wrap',
          rowGap: 6,
        }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 2, flex: 1, minWidth: 220 }}>
            <input
              value={pageName}
              onChange={(e) => setPageName(e.target.value)}
              placeholder="Page name"
              style={{
                fontSize: 15, fontWeight: 700, border: 'none', outline: 'none',
                background: 'transparent', color: 'var(--text)', padding: 0,
              }}
            />
            <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
              <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>/pages/</span>
              <input
                value={pageKey}
                onChange={(e) => { setPageKey(e.target.value); setKeyEdited(true); }}
                disabled={!isNew}
                placeholder="page-key"
                style={{
                  fontSize: 11, border: 'none', outline: 'none',
                  background: 'transparent',
                  color: isNew ? 'var(--primary)' : 'var(--text-muted)',
                  padding: 0, width: 180,
                  cursor: isNew ? 'text' : 'default',
                }}
              />
              {!isNew && <span style={{ fontSize: 10, color: 'var(--text-muted)' }}>(key locked after first save)</span>}
            </div>
          </div>

          {saveMsg && (
            <span style={{
              fontSize: 12,
              color: saveState === 'error' ? '#dc2626' : '#059669',
              fontWeight: 600,
            }}>
              {saveState === 'saved' ? '✓ ' : saveState === 'error' ? '✕ ' : ''}{saveMsg}
            </span>
          )}

          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn-ghost" onClick={handleSave} disabled={isBusy}>
              {isBusy ? 'Saving…' : 'Save Draft'}
            </button>
            <button className="btn-primary" onClick={handlePublish} disabled={isBusy}>
              Publish
            </button>
          </div>
        </div>

        {/* Three-panel layout */}
        <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
          <WidgetPalette />
          <PageCanvas
            sections={sections}
            selectedWidgetId={selectedWidgetId}
            onAddSection={addSection}
            onDropWidget={dropWidget}
            onSelectWidget={setSelectedWidgetId}
            onDeleteWidget={deleteWidget}
            onMoveWidget={moveWidget}
            onChangeSectionTitle={changeSectionTitle}
            onChangeSectionColumns={changeSectionColumns}
            onDeleteSection={deleteSection}
          />
          <WidgetEditor
            widget={selectedWidget}
            maxColSpan={selectedSectionColumns}
            onChange={updateWidget}
          />
        </div>
      </div>
    </DndProvider>
  );
}
