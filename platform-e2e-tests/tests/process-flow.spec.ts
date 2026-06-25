import { test, expect } from '@playwright/test'
import { getToken } from './helpers/auth'

const apiBase = process.env.API_BASE_URL ?? 'http://localhost:8000'

test.describe('Process API Flow', () => {
  let token: string
  let processId: string

  test.beforeAll(async () => {
    token = await getToken()
  })

  test('start a process and get 201', async ({ request }) => {
    const res = await request.post(`${apiBase}/api/v1/processes`, {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        processKey: 'e2e-test-process',
        businessKey: `e2e-${Date.now()}`,
        variables: { testRun: true },
      },
    })
    expect([200, 201, 202]).toContain(res.status())
    const body = await res.json()
    processId = body.data?.id ?? body.id
    expect(processId).toBeTruthy()
  })

  test('get process by ID returns 200', async ({ request }) => {
    if (!processId) test.skip()
    const res = await request.get(`${apiBase}/api/v1/processes/${processId}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    expect(res.status()).toBe(200)
  })

  test('list processes returns paginated response', async ({ request }) => {
    const res = await request.get(`${apiBase}/api/v1/processes?pageSize=5`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    expect(res.status()).toBe(200)
    const body = await res.json()
    const content = body.data?.content ?? body.content
    expect(Array.isArray(content)).toBe(true)
  })
})
