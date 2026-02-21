import { useState, useCallback } from 'react'
import './App.css'
import DomainSelector from './components/DomainSelector'
import PromptSuggestions from './components/PromptSuggestions'
import PromptLibrary from './components/PromptLibrary'
import QueryForm from './components/QueryForm'
import ResultTable from './components/ResultTable'
import ResultChart from './components/ResultChart'
import SavePromptButton from './components/SavePromptButton'
import ExportButtons from './components/ExportButtons'
import SpeakButton from './components/SpeakButton'

const API_URL = import.meta.env.VITE_API_URL || ''

function App() {
  const [question, setQuestion] = useState('')
  const [response, setResponse] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [selectedDomain, setSelectedDomain] = useState(null)
  const [viewMode, setViewMode] = useState('table')
  const [manualChartType, setManualChartType] = useState(null)
  const [showLibrary, setShowLibrary] = useState(false)
  const [refreshKey, setRefreshKey] = useState(0)

  const refresh = useCallback(() => setRefreshKey((k) => k + 1), [])

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!question.trim()) return

    setLoading(true)
    setError(null)
    setResponse(null)
    setViewMode('table')
    setManualChartType(null)

    try {
      const body = { question: question.trim() }
      if (selectedDomain) body.domainId = selectedDomain
      const res = await fetch(`${API_URL}/api/query`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
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

      <DomainSelector selectedDomain={selectedDomain} onSelect={setSelectedDomain} refreshKey={refreshKey} />

      <div className="prompt-bar">
        <PromptSuggestions domainId={selectedDomain} onSelect={(q) => setQuestion(q)} refreshKey={refreshKey} />
        <button className="library-toggle" onClick={() => setShowLibrary(true)}>Bibliothèque</button>
      </div>

      {showLibrary && (
        <PromptLibrary
          domainId={selectedDomain}
          onSelect={(q) => setQuestion(q)}
          onClose={() => setShowLibrary(false)}
          onChanged={refresh}
        />
      )}

      <QueryForm question={question} setQuestion={setQuestion} onSubmit={handleSubmit} loading={loading} />

      {error && (
        <div className="error-box">
          <strong>Erreur :</strong> {error}
        </div>
      )}

      {response && (
        <div className="results">
          {response.answer && (
            <section className="result-section">
              <div className="answer-header">
                <h2>Réponse</h2>
                <div className="speak-controls">
                  <SpeakButton text={response.answer} />
                  <SpeakButton text={response.answer} voiceMode="wookiee" />
                  <SpeakButton text={response.answer} voiceMode="r2d2" />
                </div>
              </div>
              <p className="answer">{response.answer}</p>
            </section>
          )}

          {response.generatedSql && (
            <section className="result-section">
              <h2>SQL généré</h2>
              <pre className="sql-block"><code>{response.generatedSql}</code></pre>
              <SavePromptButton
                question={question}
                sql={response.generatedSql}
                domainId={selectedDomain}
                onSaved={refresh}
              />
            </section>
          )}

          {response.results && response.results.length > 0 && (
            <section className="result-section">
              <div className="result-header">
                <h2>Résultats ({response.results.length} ligne{response.results.length > 1 ? 's' : ''})</h2>
                <div className="result-actions">
                  <div className="view-toggle">
                    <button className={viewMode === 'table' ? 'active' : ''} onClick={() => setViewMode('table')}>Tableau</button>
                    <button className={viewMode === 'chart' ? 'active' : ''} onClick={() => setViewMode('chart')}>Graphique</button>
                  </div>
                  <ExportButtons results={response.results} />
                </div>
              </div>
              {viewMode === 'table' ? (
                <ResultTable results={response.results} />
              ) : (
                <ResultChart
                  results={response.results}
                  chartType={manualChartType}
                  onChartTypeChange={setManualChartType}
                />
              )}
            </section>
          )}
        </div>
      )}
    </div>
  )
}

export default App
