export interface Task {
  id: string
  name: string
  processKey: string
  businessKey?: string
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  dueDate?: string
  status: 'OPEN' | 'CLAIMED' | 'COMPLETED'
  assignee?: string
  tenantId: string
}

export interface ProcessInstance {
  id: string
  processKey: string
  businessKey?: string
  status: 'PENDING' | 'ACTIVE' | 'COMPLETED' | 'FAILED' | 'TERMINATED'
  startedAt: string
  endedAt?: string
  tenantId: string
}

export interface ProcessHistoryEntry {
  id: string
  activityId: string
  activityName: string
  activityType: string
  startTime: string
  endTime?: string
  durationMs?: number
}

export interface Page<T> {
  content: T[]
  cursor?: string
  hasMore: boolean
  totalElements?: number
}
