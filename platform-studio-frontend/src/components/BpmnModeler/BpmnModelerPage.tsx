import React, { useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import BpmnModeler, { BpmnModelerHandle } from './BpmnModeler';
import { useArtifactStore } from '../../store/useArtifactStore';
import { artifactApi, ArtifactContent } from '../../api/artifactApi';

const toolbar: React.CSSProperties = {
  display: 'flex', gap: 8, marginBottom: 12, alignItems: 'center',
};
const editorWrap: React.CSSProperties = {
  flex: 1, border: '1px solid var(--border)', borderRadius: 8,
  overflow: 'hidden', height: 'calc(100vh - 160px)',
};

export default function BpmnModelerPage() {
  const { id } = useParams<{ id?: string }>();
  const modelerRef = useRef<BpmnModelerHandle>(null);
  const { saveArtifact, publishArtifact } = useArtifactStore();

  const [artifactContent, setArtifactContent] = useState<ArtifactContent | null>(null);
  const [dirty, setDirty] = useState(false);
  const [name, setName] = useState('new-process');
  const [publishVersion, setPublishVersion] = useState('');
  const [showPublish, setShowPublish] = useState(false);
  const [status, setStatus] = useState('');

  useEffect(() => {
    if (!id) return;
    artifactApi.getContent(id).then(setArtifactContent).catch(console.error);
    setName('');
  }, [id]);

  const handleSave = async () => {
    if (!modelerRef.current) return;
    const xml = await modelerRef.current.getXml();
    const artifactName = artifactContent?.metadata.name ?? name;
    try {
      await saveArtifact('BPMN', artifactName, xml, artifactName);
      setDirty(false);
      setStatus('Saved');
      setTimeout(() => setStatus(''), 2000);
    } catch (e) {
      setStatus('Save failed');
    }
  };

  const handlePublish = async () => {
    const currentId = id ?? artifactContent?.metadata.id;
    if (!currentId || !publishVersion) return;
    try {
      await publishArtifact(currentId, publishVersion);
      setShowPublish(false);
      setStatus(`Published v${publishVersion}`);
      setTimeout(() => setStatus(''), 3000);
    } catch (e) {
      setStatus('Publish failed');
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div style={toolbar}>
        {!id && (
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Process name"
            style={{
              padding: '6px 10px', border: '1px solid var(--border)',
              borderRadius: 6, fontSize: 13, width: 200,
            }}
          />
        )}
        <button className="btn-primary" onClick={handleSave}>
          {dirty ? '● Save' : 'Save'}
        </button>
        {(id || artifactContent) && (
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
          {artifactContent?.metadata.name ?? 'New BPMN Process'}
          {artifactContent?.metadata.currentVersion && ` v${artifactContent.metadata.currentVersion}`}
        </span>
      </div>
      <div style={editorWrap}>
        <BpmnModeler
          ref={modelerRef}
          initialXml={artifactContent?.content ?? undefined}
          onDirty={() => setDirty(true)}
        />
      </div>
    </div>
  );
}
