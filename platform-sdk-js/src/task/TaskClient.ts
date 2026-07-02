import { PlatformClient } from '../client/PlatformClient'
import { TaskCompletionBuilder } from './TaskCompletionBuilder'
import { Task } from './types'
import { Page } from '../process/types'

export class TaskClient {
  constructor(private readonly client: PlatformClient) {}

  async inbox(params?: { group?: string; processKey?: string; priority?: string; pageSize?: number; cursor?: string }): Promise<Page<Task>> {
    const qs = new URLSearchParams()
    if (params?.group) qs.set('group', params.group)
    if (params?.processKey) qs.set('processKey', params.processKey)
    if (params?.priority) qs.set('priority', params.priority)
    if (params?.pageSize) qs.set('pageSize', String(params.pageSize))
    if (params?.cursor) qs.set('cursor', params.cursor)
    return this.client.get<Page<Task>>(`/v1/tasks?${qs}`)
  }

  async claim(taskId: string): Promise<Task> {
    return this.client.post<Task>(`/v1/tasks/${taskId}/claim`)
  }

  complete(taskId: string): TaskCompletionBuilder {
    return new TaskCompletionBuilder(this.client, taskId)
  }

  async reassign(taskId: string, assignTo: string, reason: string): Promise<void> {
    return this.client.post(`/v1/tasks/${taskId}/reassign`, { assignTo, reason })
  }
}
