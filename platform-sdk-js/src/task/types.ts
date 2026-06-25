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
