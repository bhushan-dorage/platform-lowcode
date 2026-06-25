import { PlatformClientConfig } from './PlatformClient'

interface TokenResponse {
  access_token: string
  expires_in: number
}

export class TokenManager {
  private token: string | null = null
  private expiresAt: number = 0

  constructor(private readonly config: PlatformClientConfig) {}

  async getToken(): Promise<string> {
    if (this.token && Date.now() < this.expiresAt - 60_000) {
      return this.token
    }
    return this.refreshToken()
  }

  private async refreshToken(): Promise<string> {
    const body = new URLSearchParams({
      grant_type: 'client_credentials',
      client_id: this.config.clientId,
      client_secret: this.config.clientSecret,
    })
    const res = await fetch(this.config.tokenUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: body.toString(),
    })
    if (!res.ok) {
      throw new Error(`Token refresh failed: HTTP ${res.status}`)
    }
    const data = (await res.json()) as TokenResponse
    this.token = data.access_token
    this.expiresAt = Date.now() + data.expires_in * 1000
    return this.token
  }
}
