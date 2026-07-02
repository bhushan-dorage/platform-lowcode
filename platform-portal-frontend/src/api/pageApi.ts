import client from './client'
import { PageDefinitionDto } from '../types/pageSchema'

export const pageApi = {
  get: (pageKey: string) =>
    client.get<{ data: PageDefinitionDto }>(`/v1/pages/${pageKey}`).then(r => r.data.data),
  list: () =>
    client.get<{ data: { data: PageDefinitionDto[] } }>('/v1/pages').then(r => r.data.data.data),
}
