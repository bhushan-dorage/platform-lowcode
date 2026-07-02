import { TokenManager } from './TokenManager'
import {
  PlatformSdkError,
  EntityNotFoundError,
  AccessDeniedError,
  ValidationError,
  RateLimitError,
  TaskAlreadyClaimedError,
} from './errors'

export interface PlatformClientConfig {
  baseUrl: string
  tokenUrl: string
  clientId: string
  clientSecret: string
  maxRetries?: number
  timeoutMs?: number
}

export class PlatformClient {
  private readonly tokenManager: TokenManager
  private readonly maxRetries: number

  constructor(private readonly config: PlatformClientConfig) {
    this.tokenManager = new TokenManager(config)
    this.maxRetries = config.maxRetries ?? 3
  }

  async get<T>(path: string): Promise<T> {
    return this.request<T>('GET', path)
  }

  async post<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>('POST', path, body)
  }

  async put<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>('PUT', path, body)
  }

  async delete(path: string): Promise<void> {
    await this.request<void>('DELETE', path)
  }

  private async request<T>(method: string, path: string, body?: unknown): Promise<T> {
    const token = await this.tokenManager.getToken()
    const url = this.config.baseUrl + path

    let lastError: Error | null = null
    for (let attempt = 0; attempt <= this.maxRetries; attempt++) {
      if (attempt > 0) {
        const backoffMs = Math.pow(2, attempt - 1) * 1000
        await sleep(backoffMs)
      }
      try {
        const res = await fetch(url, {
          method,
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json',
          },
          body: body !== undefined ? JSON.stringify(body) : undefined,
        })

        if (res.status === 429) {
          const retryAfter = Number(res.headers.get('Retry-After') ?? '60')
          throw new RateLimitError('Rate limited', retryAfter)
        }

        if (res.status >= 500 && attempt < this.maxRetries) {
          lastError = new PlatformSdkError(`Server error: HTTP ${res.status}`, res.status)
          continue
        }

        if (!res.ok) {
          const text = await res.text().catch(() => '')
          throwForStatus(res.status, text)
        }

        if (res.status === 204 || res.headers.get('Content-Length') === '0') {
          return undefined as T
        }

        const json = await res.json() as Record<string, unknown>
        return (json['data'] ?? json) as T
      } catch (e) {
        if (e instanceof PlatformSdkError) throw e
        lastError = e as Error
      }
    }
    throw lastError ?? new Error('Unknown error after retries')
  }
}

function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms))
}

function throwForStatus(status: number, body: string): never {
  let message = body
  try { message = JSON.parse(body).message ?? body } catch {}

  switch (status) {
    case 400: throw new ValidationError(message)
    case 403: throw new AccessDeniedError(message)
    case 404: throw new EntityNotFoundError(message)
    case 409: throw new TaskAlreadyClaimedError(message)
    default: throw new PlatformSdkError(`HTTP ${status}: ${message}`, status)
  }
}
