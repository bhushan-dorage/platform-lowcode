import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import FieldPalette from './FieldPalette';
import FormCanvas from './FormCanvas';
import FieldEditor from './FieldEditor';
import { FieldDefinition, FieldType } from './types';
import { useArtifactStore } from '../../store/useArtifactStore';
import { artifactApi } from '../../api/artifactApi';

function genId() { return Math.random().toString(36).slice(2, 9); }

function makeField(type: FieldType): FieldDefinition {
  return {
    id: genId(),
    type,
    label: type.charAt(0).toUpperCase() + type.slice(1),
    name: type + '_' + genId(),
    required: false,
  };
}

export default function FormDesignerPage() {
  const { id } = useParams<{ id?: string }>();
  const { saveArtifact, publishArtifact } = useArtifactStore();

  const [fields, setFields] = useState<FieldDefinition[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [formName, setFormName] = useState('new-form');
  const [savedId, setSavedId] = useState<string | null>(id ?? null);
  const [publishVersion, setPublishVersion] = useState('');
  const [showPublish, setShowPublish] = useState(false);
  const [status, setStatus] = useState('');

  useEffect(() => {
    if (!id) return;
    artifactApi.getContent(id).then((ac) => {
      if (ac.content) {
        try {
          const schema = JSON.parse(ac.content);
          setFields(schema.fields ?? []);
          setFormName(ac.metadata.name);
        } catch {
          /* ignore parse errors */
        }
      }
    }).catch(console.error);
  }, [id]);

  const handleDrop = (fieldType: FieldType) => {
    setFields((prev) => [...prev, makeField(fieldType)]);
  };

  const handleDelete = (fieldId: string) => {
    setFields((prev) => prev.filter((f) => f.id !== fieldId));
    if (selectedId === fieldId) setSelectedId(null);
  };

  const handleMove = (fieldId: string, direction: 'up' | 'down') => {
    setFields((prev) => {
      const idx = prev.findIndex((f) => f.id === fieldId);
      if (idx < 0) return prev;
      const next = [...prev];
      const target = direction === 'up' ? idx - 1 : idx + 1;
      if (target < 0 || target >= next.length) return prev;
      [next[idx], next[target]] = [next[target], next[idx]];
      return next;
    });
  };

  const handleFieldChange = (updated: FieldDefinition) => {
    setFields((prev) => prev.map((f) => (f.id === updated.id ? updated : f)));
  };

  const handleSave = async () => {
    const schema = { formKey: formName, fields };
    const content = JSON.stringify(schema, null, 2);
    try {
      const saved = await saveArtifact('FORM', formName, content, formName);
      setSavedId(saved.id);
      setStatus('Saved');
      setTimeout(() => setStatus(''), 2000);
    } catch {
      setStatus('Save failed');
    }
  };

  const handlePublish = async () => {
    if (!savedId || !publishVersion) return;
    try {
      await publishArtifact(savedId, publishVersion);
      setShowPublish(false);
      setStatus(`Published v${publishVersion}`);
      setTimeout(() => setStatus(''), 3000);
    } catch {
      setStatus('Publish failed');
    }
  };

  const selectedField = fields.find((f) => f.id === selectedId) ?? null;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div style={{ display: 'flex', gap: 8, marginBottom: 12, alignItems: 'center' }}>
        <input
          value={formName}
          onChange={(e) => setFormName(e.target.value)}
          placeholder="Form name"
          style={{ padding: '6px 10px', border: '1px solid var(--border)', borderRadius: 6, fontSize: 13, width: 200 }}
        />
        <button className="btn-primary" onClick={handleSave}>Save Form</button>
        {savedId && (
          <button className="btn-ghost" onClick={() => setShowPublish(!showPublish)}>
            Publish
          </button>
        )}
        {showPublish && (
          <>
            <input
              value={publishVersion}
              onChange={(e) => setPublishVersion(e.target.value)}
              placeholder="1.0.0"
              style={{ padding: '6px 10px', border: '1px solid var(--border)', borderRadius: 6, fontSize: 13, width: 100 }}
            />
            <button className="btn-primary" onClick={handlePublish}>Confirm</button>
          </>
        )}
        {status && <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>{status}</span>}
        <span style={{ marginLeft: 'auto', fontSize: 12, color: 'var(--text-muted)' }}>
          {fields.length} field{fields.length !== 1 ? 's' : ''}
        </span>
      </div>
      <div style={{ display: 'flex', flex: 1, border: '1px solid var(--border)', borderRadius: 8, overflow: 'hidden' }}>
        <FieldPalette />
        <FormCanvas
          fields={fields}
          selectedId={selectedId}
          onDrop={handleDrop}
          onSelect={setSelectedId}
          onDelete={handleDelete}
          onMove={handleMove}
        />
        <FieldEditor field={selectedField} onChange={handleFieldChange} />
      </div>
    </div>
  );
}
