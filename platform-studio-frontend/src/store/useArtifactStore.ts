import { create } from 'zustand';
import { Artifact, artifactApi } from '../api/artifactApi';

interface ArtifactState {
  artifacts: Artifact[];
  current: Artifact | null;
  loading: boolean;
  error: string | null;
  fetchArtifacts: (type?: string) => Promise<void>;
  selectArtifact: (id: string) => void;
  saveArtifact: (type: string, name: string, content: string, displayName?: string) => Promise<Artifact>;
  publishArtifact: (id: string, version: string) => Promise<void>;
}

export const useArtifactStore = create<ArtifactState>((set, get) => ({
  artifacts: [],
  current: null,
  loading: false,
  error: null,

  fetchArtifacts: async (type) => {
    set({ loading: true, error: null });
    try {
      const artifacts = await artifactApi.list(type);
      set({ artifacts, loading: false });
    } catch (e: unknown) {
      set({ error: (e as Error).message, loading: false });
    }
  },

  selectArtifact: (id) => {
    const found = get().artifacts.find((a) => a.id === id) ?? null;
    set({ current: found });
  },

  saveArtifact: async (type, name, content, displayName) => {
    const saved = await artifactApi.save({ type, name, content, displayName });
    set((state) => ({
      artifacts: state.artifacts.some((a) => a.id === saved.id)
        ? state.artifacts.map((a) => (a.id === saved.id ? saved : a))
        : [...state.artifacts, saved],
      current: saved,
    }));
    return saved;
  },

  publishArtifact: async (id, version) => {
    const published = await artifactApi.publish(id, version);
    set((state) => ({
      artifacts: state.artifacts.map((a) => (a.id === id ? published : a)),
      current: state.current?.id === id ? published : state.current,
    }));
  },
}));
