import { PlatformClient } from '../client/PlatformClient'
import { ProcessStartBuilder } from './ProcessStartBuilder'
import { ProcessInstance, Page } from './types'

export class ProcessClient {
  constructor(private readonly client: PlatformClient) {}

  start(processKey: string): ProcessStartBuilder {
    return new ProcessStartBuilder(this.client, processKey)
  }

  async findById(processId: string): Promise<ProcessInstance> {
    return this.client.get<ProcessInstance>(`/v1/processes/${processId}`)
  }

  async findByBusinessKey(businessKey: string): Promise<ProcessInstance> {
    return this.client.get<ProcessInstance>(`/v1/processes?businessKey=${encodeURIComponent(businessKey)}`)
  }

  async list(params?: { status?: string; processKey?: string; pageSize?: number; cursor?: string }): Promise<Page<ProcessInstance>> {
    const qs = new URLSearchParams()
    if (params?.status) qs.set('status', params.status)
    if (params?.processKey) qs.set('processKey', params.processKey)
    if (params?.pageSize) qs.set('pageSize', String(params.pageSize))
    if (params?.cursor) qs.set('cursor', params.cursor)
    return this.client.get<Page<ProcessInstance>>(`/v1/processes?${qs}`)
  }

  async signal(processId: string, signalName: string, variables?: Record<string, unknown>): Promise<void> {
    return this.client.post(`/v1/processes/${processId}/signal/${signalName}`, variables ?? {})
  }

  async terminate(processId: string, reason: string): Promise<void> {
    return this.client.post(`/v1/processes/${processId}/terminate`, { reason })
  }
}
