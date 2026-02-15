import { useState, useEffect } from 'react'

const API_URL = import.meta.env.VITE_API_URL || ''

export default function SavePromptButton({ question, sql, domainId, onSaved }) {
  const [saved, setSaved] = useState(false)
  const [saving, setSaving] = useState(false)
  const [showForm, setShowForm] = useState(false)
  const [domains, setDomains] = useState([])
  const [selectedDomainId, setSelectedDomainId] = useState(domainId ?? '')
  const [newDomainName, setNewDomainName] = useState('')
  const [mode, setMode] = useState('existing') // 'existing' | 'new'

  useEffect(() => {
    if (showForm) {
      fetch(`${API_URL}/api/domains`)
        .then((res) => res.json())
        .then(setDomains)
        .catch(() => setDomains([]))
    }
  }, [showForm])

  const handleSave = async () => {
    setSaving(true)
    try {
      let targetDomainId = mode === 'existing' ? (selectedDomainId || null) : null

      if (mode === 'new' && newDomainName.trim()) {
        const domRes = await fetch(`${API_URL}/api/domains`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ name: newDomainName.trim(), description: '' }),
        })
        if (domRes.ok) {
          const created = await domRes.json()
          targetDomainId = created.id
        }
      }

      const res = await fetch(`${API_URL}/api/prompts`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ question, sqlGenerated: sql, domainId: targetDomainId }),
      })
      if (res.ok) {
        setSaved(true)
        setShowForm(false)
        if (onSaved) onSaved()
      }
    } catch {
      // silently fail
    } finally {
      setSaving(false)
    }
  }

  if (saved) return <span className="save-confirm">Prompt enregistré</span>

  if (!showForm) {
    return (
      <button className="save-btn" onClick={() => setShowForm(true)}>
        Enregistrer ce prompt
      </button>
    )
  }

  return (
    <div className="save-form">
      <div className="save-form-row">
        <label>
          <input type="radio" name="domain-mode" checked={mode === 'existing'} onChange={() => setMode('existing')} />
          Domaine existant
        </label>
        <label>
          <input type="radio" name="domain-mode" checked={mode === 'new'} onChange={() => setMode('new')} />
          Nouveau domaine
        </label>
      </div>

      {mode === 'existing' ? (
        <select value={selectedDomainId} onChange={(e) => setSelectedDomainId(e.target.value)}>
          <option value="">Sans domaine</option>
          {domains.map((d) => (
            <option key={d.id} value={d.id}>{d.name}</option>
          ))}
        </select>
      ) : (
        <input
          type="text"
          placeholder="Nom du nouveau domaine"
          value={newDomainName}
          onChange={(e) => setNewDomainName(e.target.value)}
        />
      )}

      <div className="save-form-actions">
        <button className="save-btn" onClick={handleSave} disabled={saving || (mode === 'new' && !newDomainName.trim())}>
          {saving ? 'Enregistrement...' : 'Enregistrer'}
        </button>
        <button className="cancel-btn" onClick={() => setShowForm(false)}>Annuler</button>
      </div>
    </div>
  )
}
