import { api } from './client';

export interface PageRecord {
  id: string;
  pageKey: string;
  name: string;
  description?: string;
  schema: string;
  status: 'DRAFT' | 'PUBLISHED' | 'DEPRECATED';
  version: number;
  createdAt: string;
  updatedAt: string;
}

export const pageBuilderApi = {
  create: (body: { pageKey: string; name: string; description?: string; schema: string }) =>
    api.post<PageRecord>('/pages', body),

  update: (pageKey: string, body: { name?: string; description?: string; schema?: string }) =>
    api.put<PageRecord>(`/pages/${pageKey}`, body),

  get: (pageKey: string) =>
    api.get<PageRecord>(`/pages/${pageKey}`),

  list: () =>
    api.get<{ data: PageRecord[] }>('/pages'),

  publish: (pageKey: string, schema: string) =>
    api.post<PageRecord>(`/pages/${pageKey}/publish`, { schema }),
};
