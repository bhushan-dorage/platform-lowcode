import { api } from './client';

export interface Artifact {
  id: string;
  tenantId: string;
  type: 'BPMN' | 'DMN' | 'FORM' | 'DATA_MODEL' | 'RULE_SET';
  name: string;
  displayName: string | null;
  description: string | null;
  currentVersion: string | null;
  headCommitSha: string | null;
  status: 'DRAFT' | 'PUBLISHED' | 'DEPRECATED';
  createdBy: string | null;
  createdAt: string;
  updatedAt: string;
  publishedAt: string | null;
}

export interface ArtifactContent {
  metadata: Artifact;
  content: string | null;
}

export interface DeploymentBundle {
  id: string;
  tenantId: string;
  version: string;
  artifactVersions: Record<string, string>;
  status: 'DRAFT' | 'DEPLOYING' | 'DEPLOYED' | 'FAILED';
  createdBy: string | null;
  createdAt: string;
  deployedAt: string | null;
}

export const artifactApi = {
  list: (type?: string) =>
    api.get<Artifact[]>(`/artifacts${type ? `?type=${type}` : ''}`),

  save: (req: { type: string; name: string; displayName?: string; description?: string; content: string }) =>
    api.post<Artifact>('/artifacts', req),

  getContent: (id: string, ref?: string) =>
    api.get<ArtifactContent>(`/artifacts/${id}/content${ref ? `?ref=${ref}` : ''}`),

  getVersionContent: (id: string, version: string) =>
    api.get<ArtifactContent>(`/artifacts/${id}/versions/${version}/content`),

  publish: (id: string, version: string) =>
    api.post<Artifact>(`/artifacts/${id}/publish`, { version }),

  deprecate: (id: string) =>
    api.post<Artifact>(`/artifacts/${id}/deprecate`, {}),

  listBundles: () => api.get<DeploymentBundle[]>('/bundles'),

  createBundle: (req: { version: string; artifactVersions: Record<string, string> }) =>
    api.post<DeploymentBundle>('/bundles', req),

  deployBundle: (id: string) =>
    api.post<DeploymentBundle>(`/bundles/${id}/deploy`, {}),
};
