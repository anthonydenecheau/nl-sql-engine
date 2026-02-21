import { useState, useRef, useCallback } from 'react'

const API_URL = import.meta.env.VITE_API_URL || ''

// Estime la durée de lecture TTS d'un texte français (~15 caractères/seconde)
function estimateDuration(text) {
  return Math.max(2, text.length / 15)
}

const SAMPLE_FILES = {
  wookiee: { url: '/wookiee.wav', duration: 106 },
  r2d2: { url: '/r2d2.wav', duration: 179 },
}

// voiceMode: 'normal' | 'wookiee' | 'r2d2'
export default function SpeakButton({ text, voiceMode = 'normal' }) {
  const [state, setState] = useState('idle')
  const audioRef = useRef(null)
  const audioContextRef = useRef(null)

  const stop = useCallback(() => {
    if (audioRef.current) {
      if (audioRef.current.source) {
        audioRef.current.source.stop()
      } else if (audioRef.current.pause) {
        audioRef.current.pause()
      }
    }
    if (audioContextRef.current) {
      audioContextRef.current.close()
      audioContextRef.current = null
    }
    audioRef.current = null
    setState('idle')
  }, [])

  const toggle = useCallback(async () => {
    if (state === 'playing') { stop(); return }
    if (state !== 'idle') return

    setState('loading')
    try {
      if (voiceMode !== 'normal') {
        // Jouer le vrai sample audio avec playbackRate ajusté
        const sample = SAMPLE_FILES[voiceMode]
        const targetDuration = estimateDuration(text)
        const playbackRate = sample.duration / targetDuration

        const audioContext = new AudioContext()
        audioContextRef.current = audioContext

        const res = await fetch(sample.url)
        const arrayBuffer = await res.arrayBuffer()
        const audioBuffer = await audioContext.decodeAudioData(arrayBuffer)

        const source = audioContext.createBufferSource()
        source.buffer = audioBuffer
        // Vitesse originale du sample — son authentique
        source.playbackRate.value = 1.0
        source.connect(audioContext.destination)

        source.onended = () => {
          audioContext.close()
          audioContextRef.current = null
          audioRef.current = null
          setState('idle')
        }

        // Démarrer à un offset aléatoire pour varier, couper après la durée de la réponse
        const maxOffset = Math.max(0, sample.duration - targetDuration)
        const offset = Math.random() * maxOffset
        source.start(0, offset, targetDuration)
        audioRef.current = { source }
        setState('playing')
      } else {
        // Voix normale via Piper TTS
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
        audio.onended = () => { URL.revokeObjectURL(url); audioRef.current = null; setState('idle') }
        audio.onerror = (e) => { console.error('Audio playback error:', e); URL.revokeObjectURL(url); audioRef.current = null; setState('idle') }
        audioRef.current = audio
        await audio.play()
        setState('playing')
      }
    } catch (err) {
      console.error('TTS error:', err)
      setState('idle')
    }
  }, [text, state, voiceMode, stop])

  const titles = { normal: 'Lire la réponse', wookiee: 'Rrraaaargh !', r2d2: 'Bip bip boop !' }

  const renderIcon = () => {
    if (state === 'loading') return '⏳'
    if (state === 'playing') return '⏹️'
    if (voiceMode === 'wookiee') return <img src="/icons/chewbacca.jpg" alt="Chewbacca" className="speak-icon" />
    if (voiceMode === 'r2d2') return <img src="/icons/r2d2.jpg" alt="R2-D2" className="speak-icon" />
    return '🔊'
  }

  return (
    <button
      type="button"
      className={`speak-btn${state === 'playing' ? ' speaking' : ''}${state === 'loading' ? ' loading' : ''}`}
      onClick={toggle}
      disabled={state === 'loading'}
      title={state === 'playing' ? 'Arrêter' : state === 'loading' ? 'Chargement...' : titles[voiceMode]}
    >
      {renderIcon()}
    </button>
  )
}
