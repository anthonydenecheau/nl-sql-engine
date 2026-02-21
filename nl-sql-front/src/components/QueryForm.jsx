import VoiceMicButton from './VoiceMicButton'

export default function QueryForm({ question, setQuestion, onSubmit, loading }) {
  return (
    <form className="query-form" onSubmit={onSubmit}>
      <div className="query-input-row">
        <textarea
          className="query-input"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="Posez votre question... Ex: Quels sont les personnages de Star Wars originaires de Tatooine ?"
          rows={3}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault()
              onSubmit(e)
            }
          }}
        />
        <VoiceMicButton onTranscript={(text) => setQuestion(text)} />
      </div>
      <button className="submit-btn" type="submit" disabled={loading || !question.trim()}>
        {loading ? 'Génération en cours...' : 'Exécuter'}
      </button>
    </form>
  )
}
