export interface ProcessInstance {
  id: string
  processKey: string
  businessKey?: string
  status: 'PENDING' | 'ACTIVE' | 'COMPLETED' | 'FAILED' | 'TERMINATED'
  startedAt: string
  endedAt?: string
  tenantId: string
  variables?: Record<string, unknown>
}

export interface ProcessTracker {
  trackingId: string
  statusUrl: string
  status?: string
}

export interface Page<T> {
  content: T[]
  cursor?: string
  hasMore: boolean
  totalElements?: number
}
