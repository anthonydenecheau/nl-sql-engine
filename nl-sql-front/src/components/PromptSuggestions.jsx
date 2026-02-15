import { useState, useEffect } from 'react'

const API_URL = import.meta.env.VITE_API_URL || ''

export default function PromptSuggestions({ domainId, onSelect, refreshKey }) {
  const [prompts, setPrompts] = useState([])

  useEffect(() => {
    const url = domainId
      ? `${API_URL}/api/prompts/popular?domain=${domainId}&limit=5`
      : `${API_URL}/api/prompts/popular?limit=5`
    fetch(url)
      .then((res) => res.json())
      .then(setPrompts)
      .catch(() => setPrompts([]))
  }, [domainId, refreshKey])

  const handleClick = (prompt) => {
    fetch(`${API_URL}/api/prompts/${prompt.id}/increment`, { method: 'PUT' }).catch(() => {})
    onSelect(prompt.question)
  }

  if (prompts.length === 0) return null

  return (
    <div className="prompt-suggestions">
      <span className="suggestions-label">Suggestions :</span>
      <div className="suggestions-chips">
        {prompts.map((p) => (
          <button key={p.id} className="chip" onClick={() => handleClick(p)}>
            {p.question.length > 60 ? p.question.slice(0, 57) + '...' : p.question}
          </button>
        ))}
      </div>
    </div>
  )
}
