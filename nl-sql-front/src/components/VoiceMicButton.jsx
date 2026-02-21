import { useState, useRef, useCallback } from 'react'

const API_URL = import.meta.env.VITE_API_URL || ''

export default function VoiceMicButton({ onTranscript }) {
  const [state, setState] = useState('idle') // idle | recording | transcribing
  const mediaRecorderRef = useRef(null)
  const chunksRef = useRef([])

  const toggle = useCallback(async () => {
    if (state === 'recording') {
      mediaRecorderRef.current?.stop()
      return
    }

    if (state !== 'idle') return

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      const mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm' })
      chunksRef.current = []

      mediaRecorder.ondataavailable = (e) => {
        if (e.data.size > 0) chunksRef.current.push(e.data)
      }

      mediaRecorder.onstop = async () => {
        stream.getTracks().forEach((t) => t.stop())
        setState('transcribing')

        const blob = new Blob(chunksRef.current, { type: 'audio/webm' })
        const formData = new FormData()
        formData.append('file', blob, 'recording.webm')

        try {
          const res = await fetch(`${API_URL}/api/voice/transcribe`, {
            method: 'POST',
            body: formData,
          })
          const data = await res.json()
          if (data.text) {
            onTranscript(data.text.trim())
          }
        } catch (err) {
          console.error('Transcription error:', err)
        } finally {
          setState('idle')
        }
      }

      mediaRecorderRef.current = mediaRecorder
      mediaRecorder.start()
      setState('recording')
    } catch (err) {
      console.error('Microphone access denied:', err)
      setState('idle')
    }
  }, [state, onTranscript])

  return (
    <button
      type="button"
      className={`mic-btn${state === 'recording' ? ' listening' : ''}${state === 'transcribing' ? ' transcribing' : ''}`}
      onClick={toggle}
      disabled={state === 'transcribing'}
      title={
        state === 'recording' ? 'Arrêter l\'enregistrement'
        : state === 'transcribing' ? 'Transcription en cours...'
        : 'Dicter votre question'
      }
    >
      {state === 'transcribing' ? '⏳' : '🎙️'}
    </button>
  )
}
