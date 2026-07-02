import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { pageApi } from '../api/pageApi'
import { PageSchema } from '../types/pageSchema'
import PageRenderer from '../components/PageRenderer/PageRenderer'

export default function PageView() {
  const { pageKey } = useParams<{ pageKey: string }>()
  const [schema, setSchema] = useState<PageSchema | null>(null)
  const [pageTitle, setPageTitle] = useState<string>('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!pageKey) return
    setLoading(true)
    setError(null)
    pageApi.get(pageKey)
      .then(def => {
        const parsed: PageSchema = JSON.parse(def.schema)
        setSchema(parsed)
        setPageTitle(parsed.title ?? def.name)
      })
      .catch(() => setError('Failed to load page. Please try again.'))
      .finally(() => setLoading(false))
  }, [pageKey])

  if (loading) {
    return (
      <div style={{ padding: '2rem', textAlign: 'center', color: '#6b7280' }}>
        Loading page…
      </div>
    )
  }

  if (error || !schema) {
    return (
      <div style={{ padding: '2rem', color: '#ef4444' }}>
        {error ?? 'Page not found.'}
      </div>
    )
  }

  return (
    <div style={{ padding: '1.5rem 2rem', maxWidth: '1400px', margin: '0 auto' }}>
      <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#111827', marginBottom: '1.5rem' }}>
        {pageTitle}
      </h1>
      <PageRenderer schema={schema} />
    </div>
  )
}
