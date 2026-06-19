import { create } from 'zustand';

type Section = 'artifacts' | 'bpmn' | 'dmn' | 'forms' | 'data' | 'roles';

interface StudioState {
  activeSection: Section;
  editorDirty: boolean;
  setSection: (s: Section) => void;
  setDirty: (dirty: boolean) => void;
}

export const useStudioStore = create<StudioState>((set) => ({
  activeSection: 'artifacts',
  editorDirty: false,
  setSection: (s) => set({ activeSection: s }),
  setDirty: (dirty) => set({ editorDirty: dirty }),
}));
