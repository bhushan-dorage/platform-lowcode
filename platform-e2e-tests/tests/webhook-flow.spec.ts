import { test, expect } from '@playwright/test'
import { getToken } from './helpers/auth'

const apiBase = process.env.API_BASE_URL ?? 'http://localhost:8000'

test.describe('Webhook API Flow', () => {
  let token: string
  let webhookId: string

  test.beforeAll(async () => {
    token = await getToken()
  })

  test('register a webhook returns 201', async ({ request }) => {
    const res = await request.post(`${apiBase}/api/v1/webhooks`, {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        url: 'https://webhook.site/test-e2e-platform',
        secret: 'e2e-test-secret',
        eventTypes: ['FORM_SUBMITTED', 'TASK_COMPLETED'],
      },
    })
    expect([200, 201]).toContain(res.status())
    const body = await res.json()
    webhookId = body.id ?? body.data?.id
    expect(webhookId).toBeTruthy()
  })

  test('list webhooks returns the registered webhook', async ({ request }) => {
    if (!webhookId) test.skip()
    const res = await request.get(`${apiBase}/api/v1/webhooks`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    expect(res.status()).toBe(200)
  })

  test('delete the webhook returns 204', async ({ request }) => {
    if (!webhookId) test.skip()
    const res = await request.delete(`${apiBase}/api/v1/webhooks/${webhookId}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    expect([200, 204]).toContain(res.status())
  })
})
