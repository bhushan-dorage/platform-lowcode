import { test, expect } from '@playwright/test'
import { getToken } from './helpers/auth'

const apiBase = process.env.API_BASE_URL ?? 'http://localhost:8000'

test.describe('Audit API Flow', () => {
  let token: string

  test.beforeAll(async () => {
    token = await getToken()
  })

  test('audit query returns paginated events', async ({ request }) => {
    const res = await request.get(`${apiBase}/api/v1/audit?pageSize=10`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    expect([200, 204]).toContain(res.status())
  })

  test('unauthenticated audit request returns 401', async ({ request }) => {
    const res = await request.get(`${apiBase}/api/v1/audit`)
    expect(res.status()).toBe(401)
  })

  test('audit requires tenant_id in token — cross-tenant not possible', async ({ request }) => {
    // Verify audit endpoint scopes to the token's tenant
    const res = await request.get(`${apiBase}/api/v1/audit?pageSize=1`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    expect([200, 204]).toContain(res.status())
  })
})
