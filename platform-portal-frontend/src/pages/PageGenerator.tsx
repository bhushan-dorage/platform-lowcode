import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { pageApi } from '../api/pageApi'
import { PageSchema } from '../types/pageSchema'
import PageRenderer from '../components/PageRenderer/PageRenderer'

type Step = 'input' | 'preview' | 'saving' | 'saved'

export default function PageGenerator() {
  const navigate = useNavigate()
  const [prompt, setPrompt] = useState('')
  const [step, setStep] = useState<Step>('input')
  const [error, setError] = useState<string | null>(null)
  const [generating, setGenerating] = useState(false)

  const [suggestedPageKey, setSuggestedPageKey] = useState('')
  const [suggestedName, setSuggestedName] = useState('')
  const [schemaStr, setSchemaStr] = useState('')
  const [parsedSchema, setParsedSchema] = useState<PageSchema | null>(null)

  async function handleGenerate() {
    if (!prompt.trim()) return
    setError(null)
    setGenerating(true)
    setStep('input')
    try {
      const result = await pageApi.generate(prompt)
      const schema: PageSchema = JSON.parse(result.schema)
      setSuggestedPageKey(result.suggestedPageKey)
      setSuggestedName(result.suggestedName)
      setSchemaStr(result.schema)
      setParsedSchema(schema)
      setStep('preview')
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Generation failed. Please try again.')
    } finally {
      setGenerating(false)
    }
  }

  async function handleSave() {
    setStep('saving')
    setError(null)
    try {
      await pageApi.create({
        pageKey: suggestedPageKey,
        name: suggestedName,
        description: parsedSchema?.description,
        schema: schemaStr,
      })
      setStep('saved')
      setTimeout(() => navigate(`/pages/${suggestedPageKey}`), 1200)
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : 'Failed to save. Please try again.')
      setStep('preview')
    }
  }

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '2rem' }}>
      <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#111827', marginBottom: '0.5rem' }}>
        AI Page Generator
      </h1>
      <p style={{ color: '#6b7280', marginBottom: '1.5rem' }}>
        Describe the page you need and Claude will generate a low-code page definition for you.
      </p>

      <div style={{
        background: '#fff',
        border: '1px solid #e5e7eb',
        borderRadius: '0.75rem',
        padding: '1.5rem',
        marginBottom: '1.5rem',
      }}>
        <label style={{ display: 'block', fontWeight: 600, color: '#374151', marginBottom: '0.5rem' }}>
          Describe your page
        </label>
        <textarea
          value={prompt}
          onChange={e => setPrompt(e.target.value)}
          rows={4}
          placeholder="e.g. An order management dashboard with KPIs for total orders, revenue and fulfillment rate, a table of recent orders, and a bar chart of orders by status."
          disabled={generating || step === 'saving' || step === 'saved'}
          style={{
            width: '100%',
            padding: '0.75rem',
            border: '1px solid #d1d5db',
            borderRadius: '0.5rem',
            fontSize: '0.9rem',
            resize: 'vertical',
            fontFamily: 'inherit',
            color: '#111827',
            boxSizing: 'border-box',
          }}
        />
        <div style={{ marginTop: '1rem', display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
          <button
            onClick={handleGenerate}
            disabled={!prompt.trim() || generating || step === 'saving' || step === 'saved'}
            style={{
              padding: '0.6rem 1.25rem',
              background: generating ? '#9ca3af' : '#6366f1',
              color: '#fff',
              border: 'none',
              borderRadius: '0.5rem',
              fontWeight: 600,
              cursor: generating ? 'not-allowed' : 'pointer',
              fontSize: '0.9rem',
            }}
          >
            {generating ? '⏳ Generating…' : step === 'preview' ? '↺ Regenerate' : '✨ Generate Page'}
          </button>
          {step === 'preview' && (
            <button
              onClick={handleSave}
              disabled={step !== 'preview'}
              style={{
                padding: '0.6rem 1.25rem',
                background: '#059669',
                color: '#fff',
                border: 'none',
                borderRadius: '0.5rem',
                fontWeight: 600,
                cursor: 'pointer',
                fontSize: '0.9rem',
              }}
            >
              💾 Save Page
            </button>
          )}
          {step === 'saved' && (
            <span style={{ color: '#059669', fontWeight: 600 }}>
              ✓ Saved! Redirecting…
            </span>
          )}
        </div>

        {error && (
          <div style={{
            marginTop: '1rem',
            padding: '0.75rem',
            background: '#fef2f2',
            border: '1px solid #fca5a5',
            borderRadius: '0.5rem',
            color: '#b91c1c',
            fontSize: '0.875rem',
          }}>
            {error}
          </div>
        )}
      </div>

      {step === 'preview' && parsedSchema && (
        <div>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '1rem',
            marginBottom: '1rem',
            padding: '0.75rem 1rem',
            background: '#f0fdf4',
            border: '1px solid #86efac',
            borderRadius: '0.5rem',
          }}>
            <div>
              <span style={{ fontWeight: 600, color: '#166534' }}>Preview: </span>
              <span style={{ color: '#166534' }}>{suggestedName}</span>
            </div>
            <div style={{ color: '#6b7280', fontSize: '0.8rem' }}>
              key: <code style={{ background: '#e5e7eb', padding: '0.1rem 0.3rem', borderRadius: '0.25rem' }}>{suggestedPageKey}</code>
            </div>
          </div>
          <PageRenderer schema={parsedSchema} />
        </div>
      )}
    </div>
  )
}
