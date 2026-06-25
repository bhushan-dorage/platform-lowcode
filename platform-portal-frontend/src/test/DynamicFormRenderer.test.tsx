import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import DynamicFormRenderer from '../components/DynamicFormRenderer/DynamicFormRenderer'

const basicSchema = {
  type: 'object' as const,
  properties: {
    name: { type: 'string', title: 'Name' },
    secret: { type: 'string', title: 'Secret' },
    conditionalField: { type: 'string', title: 'Conditional' },
  },
}

describe('DynamicFormRenderer', () => {
  it('renders form fields from schema', () => {
    render(
      <DynamicFormRenderer
        schema={basicSchema}
        onSubmit={vi.fn()}
      />
    )
    expect(screen.getByLabelText(/name/i)).toBeDefined()
  })

  it('hides DENY entitlement fields', () => {
    render(
      <DynamicFormRenderer
        schema={basicSchema}
        entitlements={[{ field: 'secret', permission: 'DENY' }]}
        onSubmit={vi.fn()}
      />
    )
    expect(screen.queryByLabelText(/secret/i)).toBeNull()
  })

  it('masks MASKED entitlement fields', () => {
    render(
      <DynamicFormRenderer
        schema={basicSchema}
        formData={{ secret: 'mysecret' }}
        entitlements={[{ field: 'secret', permission: 'MASKED' }]}
        onSubmit={vi.fn()}
      />
    )
    const input = screen.getByLabelText(/secret/i) as HTMLInputElement
    expect(input.value).toBe('●●●●●●')
    expect(input.disabled).toBe(true)
  })

  it('hides fields when visibility rule not met', () => {
    render(
      <DynamicFormRenderer
        schema={basicSchema}
        visibilityRules={[{
          field: 'conditionalField',
          showWhen: { dependsOn: 'name', equals: 'show' },
        }]}
        formData={{ name: 'other' }}
        onSubmit={vi.fn()}
      />
    )
    expect(screen.queryByLabelText(/conditional/i)).toBeNull()
  })
})
