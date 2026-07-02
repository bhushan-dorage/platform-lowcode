import { Page } from '@playwright/test'

export async function loginAsAdmin(page: Page) {
  const keycloakUrl = process.env.KEYCLOAK_URL ?? 'http://localhost:8080'
  const realm = process.env.KEYCLOAK_REALM ?? 'platform'

  await page.goto('/dashboard')
  // Keycloak redirects to login page
  await page.waitForURL(`${keycloakUrl}/**`)
  await page.fill('#username', process.env.TEST_USER ?? 'admin')
  await page.fill('#password', process.env.TEST_PASSWORD ?? 'admin')
  await page.click('#kc-login')
  await page.waitForURL('**/dashboard')
}

export async function getToken(): Promise<string> {
  const response = await fetch(
    `${process.env.KEYCLOAK_URL ?? 'http://localhost:8080'}/realms/${process.env.KEYCLOAK_REALM ?? 'platform'}/protocol/openid-connect/token`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'client_credentials',
        client_id: process.env.E2E_CLIENT_ID ?? 'e2e-test-client',
        client_secret: process.env.E2E_CLIENT_SECRET ?? 'e2e-secret',
      }).toString(),
    },
  )
  const data = await response.json() as { access_token: string }
  return data.access_token
}
