import { useState, useEffect } from 'react'

const API_URL = import.meta.env.VITE_API_URL || ''

export default function DomainSelector({ selectedDomain, onSelect, refreshKey }) {
  const [domains, setDomains] = useState([])

  useEffect(() => {
    fetch(`${API_URL}/api/domains`)
      .then((res) => res.json())
      .then(setDomains)
      .catch(() => setDomains([]))
  }, [refreshKey])

  if (domains.length === 0) return null

  return (
    <div className="domain-selector">
      <label htmlFor="domain-select">Domaine :</label>
      <select
        id="domain-select"
        value={selectedDomain ?? ''}
        onChange={(e) => onSelect(e.target.value ? Number(e.target.value) : null)}
      >
        <option value="">Tous les domaines</option>
        {domains.map((d) => (
          <option key={d.id} value={d.id}>{d.name}</option>
        ))}
      </select>
    </div>
  )
}
