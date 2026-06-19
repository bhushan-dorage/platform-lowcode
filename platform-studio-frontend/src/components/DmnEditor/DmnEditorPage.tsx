import React, { useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import DmnEditor, { DmnEditorHandle } from './DmnEditor';
import { useArtifactStore } from '../../store/useArtifactStore';
import { artifactApi, ArtifactContent } from '../../api/artifactApi';

export default function DmnEditorPage() {
  const { id } = useParams<{ id?: string }>();
  const editorRef = useRef<DmnEditorHandle>(null);
  const { saveArtifact, publishArtifact } = useArtifactStore();
  const [content, setContent] = useState<ArtifactContent | null>(null);
  const [dirty, setDirty] = useState(false);
  const [name, setName] = useState('new-decision');
  const [publishVersion, setPublishVersion] = useState('');
  const [showPublish, setShowPublish] = useState(false);
  const [status, setStatus] = useState('');

  useEffect(() => {
    if (!id) return;
    artifactApi.getContent(id).then(setContent).catch(console.error);
  }, [id]);

  const handleSave = async () => {
    if (!editorRef.current) return;
    const xml = await editorRef.current.getXml();
    const artifactName = content?.metadata.name ?? name;
    try {
      await saveArtifact('DMN', artifactName, xml, artifactName);
      setDirty(false);
      setStatus('Saved');
      setTimeout(() => setStatus(''), 2000);
    } catch {
      setStatus('Save failed');
    }
  };

  const handlePublish = async () => {
    const currentId = id ?? content?.metadata.id;
    if (!currentId || !publishVersion) return;
    try {
      await publishArtifact(currentId, publishVersion);
      setShowPublish(false);
      setStatus(`Published v${publishVersion}`);
      setTimeout(() => setStatus(''), 3000);
    } catch {
      setStatus('Publish failed');
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div style={{ display: 'flex', gap: 8, marginBottom: 12, alignItems: 'center' }}>
        {!id && (
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Decision name"
            style={{ padding: '6px 10px', border: '1px solid var(--border)', borderRadius: 6, fontSize: 13, width: 200 }}
          />
        )}
        <button className="btn-primary" onClick={handleSave}>{dirty ? '● Save' : 'Save'}</button>
        {(id || content) && (
          <button className="btn-ghost" onClick={() => setShowPublish(!showPublish)}>Publish</button>
        )}
        {showPublish && (
          <>
            <input value={publishVersion} onChange={(e) => setPublishVersion(e.target.value)}
              placeholder="1.0.0"
              style={{ padding: '6px 10px', border: '1px solid var(--border)', borderRadius: 6, fontSize: 13, width: 100 }} />
            <button className="btn-primary" onClick={handlePublish}>Confirm</button>
          </>
        )}
        {status && <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>{status}</span>}
        <span style={{ marginLeft: 'auto', fontSize: 12, color: 'var(--text-muted)' }}>
          {content?.metadata.name ?? 'New DMN Decision'}
        </span>
      </div>
      <div style={{ flex: 1, border: '1px solid var(--border)', borderRadius: 8, overflow: 'hidden', height: 'calc(100vh - 160px)' }}>
        <DmnEditor ref={editorRef} initialXml={content?.content ?? undefined} onDirty={() => setDirty(true)} />
      </div>
    </div>
  );
}
