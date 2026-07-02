import { PlatformClient } from '../client/PlatformClient'
import { ProcessTracker } from './types'

export class ProcessStartBuilder {
  private _businessKey?: string
  private _idempotencyKey?: string
  private _callbackUrl?: string
  private readonly _variables: Record<string, unknown> = {}

  constructor(
    private readonly client: PlatformClient,
    private readonly processKey: string,
  ) {}

  businessKey(bk: string): this { this._businessKey = bk; return this }
  idempotencyKey(ik: string): this { this._idempotencyKey = ik; return this }
  callbackUrl(url: string): this { this._callbackUrl = url; return this }
  variable(name: string, value: unknown): this { this._variables[name] = value; return this }
  variables(vars: Record<string, unknown>): this { Object.assign(this._variables, vars); return this }

  async submit(): Promise<ProcessTracker> {
    const body: Record<string, unknown> = { processKey: this.processKey }
    if (this._businessKey) body.businessKey = this._businessKey
    if (this._idempotencyKey) body.idempotencyKey = this._idempotencyKey
    if (this._callbackUrl) body.callbackUrl = this._callbackUrl
    if (Object.keys(this._variables).length > 0) body.variables = this._variables
    return this.client.post<ProcessTracker>('/v1/processes', body)
  }
}
