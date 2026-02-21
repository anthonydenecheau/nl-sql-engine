import { useState, useRef, useCallback } from 'react'

const API_URL = import.meta.env.VITE_API_URL || ''

export default function SpeakButton({ text }) {
  const [state, setState] = useState('idle') // idle | loading | playing
  const audioRef = useRef(null)

  const toggle = useCallback(async () => {
    if (state === 'playing') {
      audioRef.current?.pause()
      audioRef.current = null
      setState('idle')
      return
    }

    if (state !== 'idle') return

    setState('loading')
    try {
      const res = await fetch(`${API_URL}/api/voice/speak`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text }),
      })

      if (!res.ok) {
        console.error('TTS error: HTTP', res.status)
        setState('idle')
        return
      }

      const arrayBuffer = await res.arrayBuffer()
      const blob = new Blob([arrayBuffer], { type: 'audio/wav' })
      const url = URL.createObjectURL(blob)
      const audio = new Audio(url)

      audio.onended = () => {
        URL.revokeObjectURL(url)
        audioRef.current = null
        setState('idle')
      }

      audio.onerror = (e) => {
        console.error('Audio playback error:', e)
        URL.revokeObjectURL(url)
        audioRef.current = null
        setState('idle')
      }

      audioRef.current = audio
      await audio.play()
      setState('playing')
    } catch (err) {
      console.error('TTS error:', err)
      setState('idle')
    }
  }, [text, state])

  return (
    <button
      type="button"
      className={`speak-btn${state === 'playing' ? ' speaking' : ''}${state === 'loading' ? ' loading' : ''}`}
      onClick={toggle}
      disabled={state === 'loading'}
      title={
        state === 'playing' ? 'Arrêter la lecture'
        : state === 'loading' ? 'Chargement...'
        : 'Lire la réponse'
      }
    >
      {state === 'loading' ? '⏳' : state === 'playing' ? '⏹️' : '🔊'}
    </button>
  )
}
