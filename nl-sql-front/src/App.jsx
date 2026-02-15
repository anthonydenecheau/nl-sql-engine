import { useState } from 'react'
import './App.css'

const API_URL = import.meta.env.VITE_API_URL || ''

function App() {
  const [question, setQuestion] = useState('')
  const [response, setResponse] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!question.trim()) return

    setLoading(true)
    setError(null)
    setResponse(null)

    try {
      const res = await fetch(`${API_URL}/api/query`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ question: question.trim() }),
      })
      const data = await res.json()
      if (data.error) {
        setError(data.error)
      } else {
        setResponse(data)
      }
    } catch (err) {
      setError('Impossible de contacter le serveur. Vérifiez que le backend est lancé.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="app">
      <header className="header">
        <h1>NL-SQL Engine</h1>
        <p className="subtitle">Interrogez votre base de données en langage naturel</p>
      </header>

      <form className="query-form" onSubmit={handleSubmit}>
        <textarea
          className="query-input"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="Posez votre question... Ex: Quels sont les personnages de Star Wars originaires de Tatooine ?"
          rows={3}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault()
              handleSubmit(e)
            }
          }}
        />
        <button className="submit-btn" type="submit" disabled={loading || !question.trim()}>
          {loading ? 'Génération en cours...' : 'Exécuter'}
        </button>
      </form>

      {error && (
        <div className="error-box">
          <strong>Erreur :</strong> {error}
        </div>
      )}

      {response && (
        <div className="results">
          {response.answer && (
            <section className="result-section">
              <h2>Réponse</h2>
              <p className="answer">{response.answer}</p>
            </section>
          )}

          {response.generatedSql && (
            <section className="result-section">
              <h2>SQL généré</h2>
              <pre className="sql-block"><code>{response.generatedSql}</code></pre>
            </section>
          )}

          {response.results && response.results.length > 0 && (
            <section className="result-section">
              <h2>Résultats ({response.results.length} ligne{response.results.length > 1 ? 's' : ''})</h2>
              <div className="table-wrapper">
                <table className="results-table">
                  <thead>
                    <tr>
                      {Object.keys(response.results[0]).map((col) => (
                        <th key={col}>{col}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {response.results.map((row, i) => (
                      <tr key={i}>
                        {Object.values(row).map((val, j) => (
                          <td key={j}>{val != null ? String(val) : ''}</td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          )}
        </div>
      )}
    </div>
  )
}

export default App
