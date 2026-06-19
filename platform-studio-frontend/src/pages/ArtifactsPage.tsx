import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useArtifactStore } from '../store/useArtifactStore';
import { Artifact } from '../api/artifactApi';

const TYPE_ROUTES: Record<string, string> = {
  BPMN: '/bpmn',
  DMN: '/dmn',
  FORM: '/forms',
  DATA_MODEL: '/data',
};

const TYPE_FILTERS = ['ALL', 'BPMN', 'DMN', 'FORM', 'DATA_MODEL', 'RULE_SET'];

function ArtifactRow({ artifact, onOpen }: { artifact: Artifact; onOpen: () => void }) {
  return (
    <tr style={{ borderBottom: '1px solid var(--border)' }}>
      <td style={{ padding: '10px 16px', fontWeight: 500 }}>{artifact.name}</td>
      <td style={{ padding: '10px 16px' }}>
        <span style={{
          fontSize: 11, fontWeight: 600, padding: '2px 8px', borderRadius: 4,
          background: '#ede9fe', color: '#6d28d9',
        }}>{artifact.type}</span>
      </td>
      <td style={{ padding: '10px 16px' }}>
        <span className={`badge badge-${artifact.status.toLowerCase()}`}>{artifact.status}</span>
      </td>
      <td style={{ padding: '10px 16px', color: 'var(--text-muted)', fontSize: 12 }}>
        {artifact.currentVersion ?? '—'}
      </td>
      <td style={{ padding: '10px 16px', color: 'var(--text-muted)', fontSize: 12 }}>
        {new Date(artifact.updatedAt).toLocaleDateString()}
      </td>
      <td style={{ padding: '10px 16px' }}>
        <button className="btn-ghost" onClick={onOpen} style={{ fontSize: 12, padding: '4px 10px' }}>Open</button>
      </td>
    </tr>
  );
}

export default function ArtifactsPage() {
  const { artifacts, loading, error, fetchArtifacts } = useArtifactStore();
  const navigate = useNavigate();
  const [filter, setFilter] = useState('ALL');

  useEffect(() => {
    fetchArtifacts(filter === 'ALL' ? undefined : filter);
  }, [filter]);

  const openArtifact = (a: Artifact) => {
    const base = TYPE_ROUTES[a.type];
    if (base) navigate(`${base}/${a.id}`);
  };

  const filtered = filter === 'ALL' ? artifacts : artifacts.filter((a) => a.type === filter);

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <h2 style={{ fontSize: 18, fontWeight: 700 }}>Artifacts</h2>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn-primary" onClick={() => navigate('/bpmn')}>New BPMN</button>
          <button className="btn-ghost" onClick={() => navigate('/dmn')}>New DMN</button>
          <button className="btn-ghost" onClick={() => navigate('/forms')}>New Form</button>
        </div>
      </div>

      <div style={{ display: 'flex', gap: 6, marginBottom: 16, flexWrap: 'wrap' }}>
        {TYPE_FILTERS.map((f) => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            style={{
              padding: '5px 12px', borderRadius: 20, fontSize: 12, cursor: 'pointer',
              background: filter === f ? 'var(--primary)' : 'var(--surface)',
              color: filter === f ? 'white' : 'var(--text-muted)',
              border: `1px solid ${filter === f ? 'var(--primary)' : 'var(--border)'}`,
              fontWeight: filter === f ? 600 : 400,
            }}
          >{f}</button>
        ))}
      </div>

      {loading && <div style={{ color: 'var(--text-muted)', padding: 40, textAlign: 'center' }}>Loading…</div>}
      {error && <div style={{ color: 'var(--danger)', padding: 16 }}>{error}</div>}

      {!loading && (
        <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 10, overflow: 'hidden' }}>
          {filtered.length === 0 ? (
            <div style={{ padding: '48px 20px', textAlign: 'center', color: 'var(--text-muted)' }}>
              <div style={{ fontSize: 32, marginBottom: 8 }}>📦</div>
              <div>No artifacts yet. Create your first one above.</div>
            </div>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ background: '#f9fafb', fontSize: 11, fontWeight: 600, textTransform: 'uppercase', color: 'var(--text-muted)' }}>
                  {['Name', 'Type', 'Status', 'Version', 'Updated', ''].map((h) => (
                    <th key={h} style={{ padding: '10px 16px', textAlign: 'left' }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {filtered.map((a) => (
                  <ArtifactRow key={a.id} artifact={a} onOpen={() => openArtifact(a)} />
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}
