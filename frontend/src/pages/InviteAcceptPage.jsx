import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../lib/api'
import { useAuthStore } from '../store/authStore'

export default function InviteAcceptPage() {
  const { token } = useParams()
  const navigate = useNavigate()
  const user = useAuthStore((state) => state.user)
  const setCurrentOrgId = useAuthStore((state) => state.setCurrentOrgId)
  const [status, setStatus] = useState('pending')
  const [error, setError] = useState('')
  const acceptRequested = useRef(false)

  useEffect(() => {
    if (!user) {
      navigate(`/login?returnTo=${encodeURIComponent(`/invite/${token}`)}`, { replace: true })
      return
    }

    // Guards against firing the accept call twice — React StrictMode double-invokes
    // effects in dev, and this call isn't safe to fire concurrently (see backend note).
    if (acceptRequested.current) return
    acceptRequested.current = true

    api
      .post(`/invites/${token}/accept`)
      .then((res) => {
        setCurrentOrgId(res.data.id)
        setStatus('success')
        setTimeout(() => navigate('/dashboard', { replace: true }), 1200)
      })
      .catch((err) => {
        setStatus('error')
        setError(err.message)
      })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, token])

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50">
      <div className="w-full max-w-sm bg-white p-8 rounded-lg shadow text-center space-y-4">
        <h1 className="text-xl font-semibold text-slate-900">Joining organization…</h1>
        {status === 'pending' && <p className="text-sm text-slate-500">Hang tight, accepting your invite.</p>}
        {status === 'success' && <p className="text-sm text-green-600">You're in! Redirecting to your dashboard…</p>}
        {status === 'error' && (
          <>
            <p className="text-sm text-red-600">{error}</p>
            <Link to="/dashboard" className="text-sm text-indigo-600 font-medium">
              Go to dashboard
            </Link>
          </>
        )}
      </div>
    </div>
  )
}
