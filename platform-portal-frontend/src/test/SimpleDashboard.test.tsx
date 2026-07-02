import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import SimpleDashboard from '../components/Dashboard/SimpleDashboard'

vi.mock('../keycloak', () => ({ default: { token: 'fake', isTokenExpired: () => false } }))

const mockGet = vi.hoisted(() => vi.fn())
vi.mock('../api/client', () => ({
  default: { get: mockGet },
}))

describe('SimpleDashboard', () => {
  beforeEach(() => {
    mockGet.mockRejectedValue(new Error('no network'))
  })

  it('renders KPI card titles', () => {
    render(<SimpleDashboard />)
    expect(screen.getByText('My Open Tasks')).toBeDefined()
    expect(screen.getByText('Overdue Tasks')).toBeDefined()
    expect(screen.getByText('Completed Today')).toBeDefined()
    expect(screen.getByText('SLA Breach Rate')).toBeDefined()
  })
})
