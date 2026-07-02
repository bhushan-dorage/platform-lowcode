import Form from '@rjsf/core'
import validator from '@rjsf/validator-ajv8'
import { RJSFSchema, UiSchema } from '@rjsf/utils'

type FieldEntitlement = {
  field: string
  permission: 'ALLOW' | 'DENY' | 'MASKED'
}

type VisibilityRule = {
  field: string
  showWhen: { dependsOn: string; equals: unknown }
}

type Props = {
  schema: RJSFSchema
  uiSchema?: UiSchema
  visibilityRules?: VisibilityRule[]
  entitlements?: FieldEntitlement[]
  formData?: Record<string, unknown>
  onSubmit: (data: Record<string, unknown>) => void
}

function applyEntitlements(
  schema: RJSFSchema,
  uiSchema: UiSchema,
  entitlements: FieldEntitlement[],
  formData: Record<string, unknown>,
): { schema: RJSFSchema; uiSchema: UiSchema; formData: Record<string, unknown> } {
  const newSchema = structuredClone(schema)
  const newUi = structuredClone(uiSchema)
  const newData = structuredClone(formData)
  const props = (newSchema.properties ?? {}) as Record<string, RJSFSchema>

  for (const ent of entitlements) {
    if (ent.permission === 'DENY') {
      delete props[ent.field]
    } else if (ent.permission === 'MASKED') {
      newUi[ent.field] = { ...(newUi[ent.field] ?? {}), 'ui:disabled': true }
      if (typeof newData[ent.field] === 'string') {
        newData[ent.field] = '●●●●●●'
      }
    }
  }
  return { schema: newSchema, uiSchema: newUi, formData: newData }
}

function applyVisibilityRules(
  uiSchema: UiSchema,
  rules: VisibilityRule[],
  formData: Record<string, unknown>,
): UiSchema {
  const newUi = structuredClone(uiSchema)
  for (const rule of rules) {
    const depValue = formData[rule.showWhen.dependsOn]
    const shouldShow = depValue === rule.showWhen.equals
    if (!shouldShow) {
      newUi[rule.field] = { ...(newUi[rule.field] ?? {}), 'ui:widget': 'hidden' }
    }
  }
  return newUi
}

export default function DynamicFormRenderer({
  schema,
  uiSchema = {},
  visibilityRules = [],
  entitlements = [],
  formData: initialData = {},
  onSubmit,
}: Props) {
  const data = initialData

  const withVisibility = applyVisibilityRules(uiSchema, visibilityRules, data)
  const { schema: finalSchema, uiSchema: finalUi, formData: finalData } =
    applyEntitlements(schema, withVisibility, entitlements, data)

  return (
    <Form
      schema={finalSchema}
      uiSchema={finalUi}
      formData={finalData}
      validator={validator}
      onSubmit={({ formData }) => onSubmit(formData as Record<string, unknown>)}
    />
  )
}
