import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import axios from 'axios'
import SimpleDashboard from '../components/Dashboard/SimpleDashboard'

vi.mock('axios')
vi.mock('../keycloak', () => ({ default: { token: 'fake', isTokenExpired: () => false } }))

const mockedAxios = vi.mocked(axios.create, true)

describe('SimpleDashboard', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('renders KPI card titles', () => {
    vi.mock('../api/client', () => ({
      default: {
        get: vi.fn().mockRejectedValue(new Error('no network')),
      },
    }))
    render(<SimpleDashboard />)
    expect(screen.getByText('My Open Tasks')).toBeDefined()
    expect(screen.getByText('Overdue Tasks')).toBeDefined()
    expect(screen.getByText('Completed Today')).toBeDefined()
    expect(screen.getByText('SLA Breach Rate')).toBeDefined()
  })
})
