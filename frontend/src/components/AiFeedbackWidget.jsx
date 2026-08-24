import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { api } from '../lib/api'

export default function AiFeedbackWidget({ orgId, projectId, agentType, question, answer }) {
  const [rating, setRating] = useState(null)
  const [correction, setCorrection] = useState('')
  const [submitted, setSubmitted] = useState(false)

  const feedbackMutation = useMutation({
    mutationFn: (payload) => api.post(`/organizations/${orgId}/projects/${projectId}/ai/feedback`, payload),
    onSuccess: () => setSubmitted(true),
  })

  function handleRate(nextRating) {
    setRating(nextRating)
    if (nextRating === 'UP') {
      feedbackMutation.mutate({ agentType, question, answer, rating: nextRating })
    }
  }

  function handleSubmitCorrection(e) {
    e.preventDefault()
    feedbackMutation.mutate({ agentType, question, answer, rating: 'DOWN', correction: correction || null })
  }

  if (submitted) {
    return <p className="text-xs text-slate-400 mt-2">Thanks for the feedback.</p>
  }

  return (
    <div className="mt-2">
      <div className="flex items-center gap-2">
        <span className="text-xs text-slate-400">Was this helpful?</span>
        <button
          onClick={() => handleRate('UP')}
          disabled={feedbackMutation.isPending}
          className={`text-sm ${rating === 'UP' ? 'opacity-100' : 'opacity-50 hover:opacity-100'}`}
          aria-label="Thumbs up"
        >
          👍
        </button>
        <button
          onClick={() => handleRate('DOWN')}
          disabled={feedbackMutation.isPending}
          className={`text-sm ${rating === 'DOWN' ? 'opacity-100' : 'opacity-50 hover:opacity-100'}`}
          aria-label="Thumbs down"
        >
          👎
        </button>
      </div>

      {rating === 'DOWN' && (
        <form onSubmit={handleSubmitCorrection} className="mt-2 flex gap-2">
          <input
            placeholder="What was wrong? (optional)"
            className="flex-1 border border-slate-300 rounded-md px-2 py-1 text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500"
            value={correction}
            onChange={(e) => setCorrection(e.target.value)}
          />
          <button
            type="submit"
            disabled={feedbackMutation.isPending}
            className="text-xs text-indigo-600 font-medium disabled:opacity-50"
          >
            Send
          </button>
        </form>
      )}
    </div>
  )
}
