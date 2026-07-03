import client from './client'
import { PageDefinitionDto } from '../types/pageSchema'

export interface GeneratedPageResponse {
  suggestedPageKey: string
  suggestedName: string
  schema: string
}

export const pageApi = {
  get: (pageKey: string) =>
    client.get<{ data: PageDefinitionDto }>(`/v1/pages/${pageKey}`).then(r => r.data.data),
  list: () =>
    client.get<{ data: { data: PageDefinitionDto[] } }>('/v1/pages').then(r => r.data.data.data),
  create: (body: { pageKey: string; name: string; description?: string; schema: string }) =>
    client.post<{ data: PageDefinitionDto }>('/v1/pages', body).then(r => r.data.data),
  generate: (prompt: string) =>
    client.post<{ data: GeneratedPageResponse }>('/v1/pages/generate', { prompt }).then(r => r.data.data),
}
