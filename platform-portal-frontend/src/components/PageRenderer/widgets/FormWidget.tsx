import { useEffect, useState } from 'react'
import client from '../../../api/client'
import { FormConfig } from '../../../types/pageSchema'
import DynamicFormRenderer from '../../DynamicFormRenderer/DynamicFormRenderer'
import { RJSFSchema, UiSchema } from '@rjsf/utils'

interface FormDefinition {
  schema: RJSFSchema
  uiSchema?: UiSchema
}

interface Props {
  config: FormConfig
  title?: string
}

export default function FormWidget({ config, title }: Props) {
  const [formDef, setFormDef] = useState<FormDefinition | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [submitted, setSubmitted] = useState(false)
  const [submitError, setSubmitError] = useState(false)

  useEffect(() => {
    setLoading(true)
    setError(false)
    client.get(`/v1/forms/${config.formKey}`)
      .then(r => {
        const data = r.data
        const payload = (data && typeof data === 'object' && (data as Record<string, unknown>)['data'])
          ? (data as Record<string, unknown>)['data'] as FormDefinition
          : data as FormDefinition
        setFormDef(payload)
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false))
  }, [config.formKey])

  const handleSubmit = (formData: Record<string, unknown>) => {
    const submitUrl = config.submitUrl ?? `/api/v1/forms/${config.formKey}/submissions`
    setSubmitError(false)
    client.post(submitUrl, formData)
      .then(() => setSubmitted(true))
      .catch(() => setSubmitError(true))
  }

  if (loading) {
    return (
      <div className="form-widget">
        {title && <div className="widget-title">{title}</div>}
        <div className="skeleton skeleton-form" />
      </div>
    )
  }

  if (error || !formDef) {
    return (
      <div className="form-widget">
        {title && <div className="widget-title">{title}</div>}
        <div className="widget-error">Failed to load form.</div>
      </div>
    )
  }

  if (submitted) {
    return (
      <div className="form-widget">
        {title && <div className="widget-title">{title}</div>}
        <div className="form-success">
          {config.successMessage ?? 'Form submitted successfully.'}
        </div>
      </div>
    )
  }

  return (
    <div className="form-widget">
      {title && <div className="widget-title">{title}</div>}
      {submitError && <div className="widget-error">Submission failed. Please try again.</div>}
      <DynamicFormRenderer
        schema={formDef.schema}
        uiSchema={formDef.uiSchema}
        onSubmit={handleSubmit}
      />
    </div>
  )
}
