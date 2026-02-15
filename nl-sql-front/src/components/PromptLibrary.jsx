import { useState, useEffect, useCallback } from 'react'

const API_URL = import.meta.env.VITE_API_URL || ''

export default function PromptLibrary({ domainId, onSelect, onClose, onChanged }) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState([])
  const [loading, setLoading] = useState(false)

  const search = useCallback(() => {
    setLoading(true)
    const params = new URLSearchParams()
    if (domainId) params.set('domain', domainId)
    if (query.trim()) params.set('q', query.trim())
    fetch(`${API_URL}/api/prompts/search?${params}`)
      .then((res) => res.json())
      .then(setResults)
      .catch(() => setResults([]))
      .finally(() => setLoading(false))
  }, [domainId, query])

  useEffect(() => {
    const timer = setTimeout(search, 300)
    return () => clearTimeout(timer)
  }, [search])

  const handleSelect = (prompt) => {
    fetch(`${API_URL}/api/prompts/${prompt.id}/increment`, { method: 'PUT' }).catch(() => {})
    onSelect(prompt.question)
    onClose()
  }

  const handleDelete = async (e, id) => {
    e.stopPropagation()
    try {
      const res = await fetch(`${API_URL}/api/prompts/${id}`, { method: 'DELETE' })
      if (res.ok) {
        setResults((prev) => prev.filter((p) => p.id !== id))
        if (onChanged) onChanged()
      }
    } catch {
      // silently fail
    }
  }

  return (
    <div className="prompt-library-overlay" onClick={onClose}>
      <div className="prompt-library" onClick={(e) => e.stopPropagation()}>
        <div className="library-header">
          <h3>Bibliothèque de prompts</h3>
          <button className="close-btn" onClick={onClose}>&times;</button>
        </div>
        <input
          className="library-search"
          type="text"
          placeholder="Rechercher un prompt..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          autoFocus
        />
        <div className="library-list">
          {loading && <p className="library-empty">Recherche...</p>}
          {!loading && results.length === 0 && <p className="library-empty">Aucun prompt trouvé</p>}
          {results.map((p) => (
            <div key={p.id} className="library-item" onClick={() => handleSelect(p)}>
              <span className="library-question">{p.question}</span>
              <span className="library-meta">
                <span className="library-usage">{p.usageCount} utilisation{p.usageCount > 1 ? 's' : ''}</span>
                <button className="library-delete" onClick={(e) => handleDelete(e, p.id)} title="Supprimer">&times;</button>
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
