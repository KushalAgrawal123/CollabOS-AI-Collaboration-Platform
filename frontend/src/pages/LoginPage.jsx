import { useState } from 'react'
import { useNavigate, useSearchParams, Link } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { api } from '../lib/api'
import { useAuthStore } from '../store/authStore'

export default function LoginPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const login = useAuthStore((state) => state.login)
  const [form, setForm] = useState({ email: '', password: '' })

  const mutation = useMutation({
    mutationFn: (payload) => api.post('/auth/login', payload).then((res) => res.data),
    onSuccess: ({ token, user }) => {
      login(token, user)
      navigate(searchParams.get('returnTo') || '/dashboard')
    },
  })

  function handleSubmit(e) {
    e.preventDefault()
    mutation.mutate(form)
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50">
      <form onSubmit={handleSubmit} className="w-full max-w-sm bg-white p-8 rounded-lg shadow space-y-4">
        <h1 className="text-2xl font-semibold text-slate-900">Welcome back</h1>

        <div className="space-y-1">
          <label className="text-sm font-medium text-slate-700">Email</label>
          <input
            required
            type="email"
            className="w-full border border-slate-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
          />
        </div>

        <div className="space-y-1">
          <label className="text-sm font-medium text-slate-700">Password</label>
          <input
            required
            type="password"
            className="w-full border border-slate-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
          />
        </div>

        {mutation.isError && <p className="text-sm text-red-600">{mutation.error.message}</p>}

        <button
          type="submit"
          disabled={mutation.isPending}
          className="w-full bg-indigo-600 text-white rounded-md py-2 font-medium hover:bg-indigo-700 disabled:opacity-50"
        >
          {mutation.isPending ? 'Logging in…' : 'Log in'}
        </button>

        <p className="text-sm text-slate-600 text-center">
          Don&apos;t have an account?{' '}
          <Link
            to={searchParams.get('returnTo') ? `/register?returnTo=${encodeURIComponent(searchParams.get('returnTo'))}` : '/register'}
            className="text-indigo-600 font-medium"
          >
            Sign up
          </Link>
        </p>
      </form>
    </div>
  )
}
