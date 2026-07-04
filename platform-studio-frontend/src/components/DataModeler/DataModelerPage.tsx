import React, { useState } from 'react';
import { useArtifactStore } from '../../store/useArtifactStore';

type FieldDataType = 'string' | 'number' | 'boolean' | 'date' | 'object' | 'array';

interface EntityField {
  id: string;
  name: string;
  type: FieldDataType;
  required: boolean;
  description: string;
}

interface EntityModel {
  name: string;
  displayName: string;
  description: string;
  fields: EntityField[];
}

function genId() { return Math.random().toString(36).slice(2, 9); }

function emptyField(): EntityField {
  return { id: genId(), name: '', type: 'string', required: false, description: '' };
}

const inputStyle: React.CSSProperties = {
  padding: '6px 10px', border: '1px solid var(--border)',
  borderRadius: 6, fontSize: 13, width: '100%',
};

export default function DataModelerPage() {
  const { saveArtifact, publishArtifact } = useArtifactStore();
  const [model, setModel] = useState<EntityModel>({
    name: '', displayName: '', description: '', fields: [],
  });
  const [savedId, setSavedId] = useState<string | null>(null);
  const [publishVersion, setPublishVersion] = useState('');
  const [showPublish, setShowPublish] = useState(false);
  const [status, setStatus] = useState('');

  const updateModel = (patch: Partial<EntityModel>) => setModel((m) => ({ ...m, ...patch }));

  const addField = () => setModel((m) => ({ ...m, fields: [...m.fields, emptyField()] }));

  const updateField = (id: string, patch: Partial<EntityField>) =>
    setModel((m) => ({
      ...m,
      fields: m.fields.map((f) => (f.id === id ? { ...f, ...patch } : f)),
    }));

  const removeField = (id: string) =>
    setModel((m) => ({ ...m, fields: m.fields.filter((f) => f.id !== id) }));

  const handleSave = async () => {
    if (!model.name) { setStatus('Name required'); return; }
    const schema = {
      type: 'object',
      title: model.displayName || model.name,
      description: model.description,
      required: model.fields.filter((f) => f.required).map((f) => f.name),
      properties: Object.fromEntries(
        model.fields.map((f) => [f.name, { type: f.type, description: f.description }])
      ),
    };
    try {
      const saved = await saveArtifact('DATA_MODEL', model.name, JSON.stringify(schema, null, 2), model.displayName || model.name);
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

  return (
    <div style={{ maxWidth: 800 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <h2 style={{ fontSize: 18, fontWeight: 700 }}>Data Modeler</h2>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          {status && <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>{status}</span>}
          <button className="btn-primary" onClick={handleSave}>Save Model</button>
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
        </div>
      </div>

      <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 10, padding: 20, marginBottom: 20 }}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 16 }}>
          <div>
            <label style={{ fontSize: 12, fontWeight: 600, display: 'block', marginBottom: 4 }}>Entity Name *</label>
            <input style={inputStyle} value={model.name} onChange={(e) => updateModel({ name: e.target.value })} placeholder="e.g. Invoice" />
          </div>
          <div>
            <label style={{ fontSize: 12, fontWeight: 600, display: 'block', marginBottom: 4 }}>Display Name</label>
            <input style={inputStyle} value={model.displayName} onChange={(e) => updateModel({ displayName: e.target.value })} placeholder="e.g. Invoice Document" />
          </div>
        </div>
        <div>
          <label style={{ fontSize: 12, fontWeight: 600, display: 'block', marginBottom: 4 }}>Description</label>
          <input style={inputStyle} value={model.description} onChange={(e) => updateModel({ description: e.target.value })} />
        </div>
      </div>

      <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 10, padding: 20 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <h3 style={{ fontWeight: 600, fontSize: 15 }}>Fields</h3>
          <button className="btn-ghost" onClick={addField} style={{ fontSize: 13 }}>+ Add Field</button>
        </div>

        {model.fields.length === 0 && (
          <div style={{ textAlign: 'center', padding: '32px 0', color: 'var(--text-muted)', fontSize: 13 }}>
            No fields yet. Click "Add Field" to define the entity schema.
          </div>
        )}

        {model.fields.map((field) => (
          <div key={field.id} style={{
            display: 'grid', gridTemplateColumns: '2fr 1.5fr 120px auto auto',
            gap: 8, alignItems: 'center', marginBottom: 8, padding: '8px 0',
            borderBottom: '1px solid var(--border)',
          }}>
            <input style={{ ...inputStyle, width: '100%' }} value={field.name} placeholder="field_name"
              onChange={(e) => updateField(field.id, { name: e.target.value })} />
            <select style={{ ...inputStyle, width: '100%' }} value={field.type}
              onChange={(e) => updateField(field.id, { type: e.target.value as FieldDataType })}>
              {(['string', 'number', 'boolean', 'date', 'object', 'array'] as FieldDataType[]).map((t) => (
                <option key={t} value={t}>{t}</option>
              ))}
            </select>
            <input style={{ ...inputStyle, width: '100%', fontSize: 12 }} value={field.description}
              placeholder="description" onChange={(e) => updateField(field.id, { description: e.target.value })} />
            <label style={{ display: 'flex', alignItems: 'center', gap: 4, fontSize: 13, whiteSpace: 'nowrap' }}>
              <input type="checkbox" checked={field.required} onChange={(e) => updateField(field.id, { required: e.target.checked })} />
              Req
            </label>
            <button onClick={() => removeField(field.id)}
              style={{ background: '#fee2e2', color: '#dc2626', border: 'none', borderRadius: 4, padding: '4px 8px', cursor: 'pointer', fontSize: 12 }}>
              ✕
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}
