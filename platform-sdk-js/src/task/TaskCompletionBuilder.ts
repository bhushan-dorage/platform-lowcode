import { PlatformClient } from '../client/PlatformClient'

export class TaskCompletionBuilder {
  private _outcome?: string
  private readonly _variables: Record<string, unknown> = {}

  constructor(private readonly client: PlatformClient, private readonly taskId: string) {}

  outcome(outcome: string): this { this._outcome = outcome; return this }
  variable(name: string, value: unknown): this { this._variables[name] = value; return this }
  formData(data: Record<string, unknown>): this { Object.assign(this._variables, data); return this }

  async submit(): Promise<void> {
    const body: Record<string, unknown> = {}
    if (this._outcome) body.outcome = this._outcome
    if (Object.keys(this._variables).length > 0) body.variables = this._variables
    return this.client.post(`/v1/tasks/${this.taskId}/complete`, body)
  }
}
