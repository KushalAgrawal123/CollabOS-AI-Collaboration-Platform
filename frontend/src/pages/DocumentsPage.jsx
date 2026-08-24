import { useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import { useAuthStore } from '../store/authStore'
import AiFeedbackWidget from '../components/AiFeedbackWidget'

function formatBytes(bytes) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export default function DocumentsPage() {
  const { orgId, projectId } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const user = useAuthStore((state) => state.user)
  const logout = useAuthStore((state) => state.logout)
  const fileInputRef = useRef(null)

  const [taskId, setTaskId] = useState('')
  const [error, setError] = useState('')
  const [question, setQuestion] = useState('')
  const [askResult, setAskResult] = useState(null)
  const [summaries, setSummaries] = useState({})

  const orgsQuery = useQuery({
    queryKey: ['organizations'],
    queryFn: () => api.get('/organizations').then((res) => res.data),
  })
  const role = orgsQuery.data?.find((o) => o.id === Number(orgId))?.role
  const canEdit = role && role !== 'VIEWER'
  const isAdmin = role === 'OWNER' || role === 'ADMIN'

  const projectQuery = useQuery({
    queryKey: ['project', orgId, projectId],
    queryFn: () => api.get(`/organizations/${orgId}/projects/${projectId}`).then((res) => res.data),
  })

  const tasksQuery = useQuery({
    queryKey: ['tasks', orgId, projectId],
    queryFn: () => api.get(`/organizations/${orgId}/projects/${projectId}/tasks`).then((res) => res.data),
  })

  const documentsQuery = useQuery({
    queryKey: ['documents', orgId, projectId],
    queryFn: () => api.get(`/organizations/${orgId}/projects/${projectId}/documents`).then((res) => res.data),
  })

  const uploadMutation = useMutation({
    mutationFn: (file) => {
      const formData = new FormData()
      formData.append('file', file)
      if (taskId) formData.append('taskId', taskId)
      // The api instance defaults Content-Type to application/json; clearing it here lets
      // the browser set multipart/form-data with the correct boundary itself.
      return api.post(`/organizations/${orgId}/projects/${projectId}/documents`, formData, {
        headers: { 'Content-Type': undefined },
      })
    },
    onSuccess: () => {
      setError('')
      setTaskId('')
      if (fileInputRef.current) fileInputRef.current.value = ''
      queryClient.invalidateQueries({ queryKey: ['documents', orgId, projectId] })
    },
    onError: (err) => setError(err.message),
  })

  const aiStatusQuery = useQuery({
    queryKey: ['ai-status', orgId],
    queryFn: () => api.get(`/organizations/${orgId}/ai/status`).then((res) => res.data),
  })

  const askMutation = useMutation({
    mutationFn: (q) =>
      api.post(`/organizations/${orgId}/projects/${projectId}/ai/ask`, { question: q }).then((res) => res.data),
    onSuccess: (data, q) => setAskResult({ ...data, question: q }),
    onError: (err) => setError(err.message),
  })

  const summarizeMutation = useMutation({
    mutationFn: (documentId) =>
      api.post(`/organizations/${orgId}/projects/${projectId}/documents/${documentId}/ai/summarize`).then((res) => res.data),
    onSuccess: (data, documentId) => setSummaries((prev) => ({ ...prev, [documentId]: data })),
    onError: (err) => setError(err.message),
  })

  const deleteMutation = useMutation({
    mutationFn: (documentId) => api.delete(`/organizations/${orgId}/projects/${projectId}/documents/${documentId}`),
    onSuccess: () => {
      setError('')
      queryClient.invalidateQueries({ queryKey: ['documents', orgId, projectId] })
    },
    onError: (err) => setError(err.message),
  })

  async function handleDownload(doc) {
    try {
      const res = await api.get(`/organizations/${orgId}/projects/${projectId}/documents/${doc.id}/download`, {
        responseType: 'blob',
      })
      const url = window.URL.createObjectURL(res.data)
      const link = document.createElement('a')
      link.href = url
      link.download = doc.originalFileName
      link.click()
      window.URL.revokeObjectURL(url)
    } catch {
      setError('Failed to download the file')
    }
  }

  function handleFileChange(e) {
    const file = e.target.files?.[0]
    if (file) uploadMutation.mutate(file)
  }

  function handleLogout() {
    logout()
    navigate('/login')
  }

  function handleAsk(e) {
    e.preventDefault()
    if (!question.trim()) return
    setAskResult(null)
    askMutation.mutate(question)
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="bg-white border-b border-slate-200">
        <div className="max-w-4xl mx-auto px-6 py-4 flex items-center justify-between gap-4 flex-wrap">
          <div className="flex items-center gap-3 min-w-0">
            <Link to={`/organizations/${orgId}/projects/${projectId}`} className="text-sm text-indigo-600 font-medium shrink-0">
              ← Board
            </Link>
            <h1 className="text-lg font-semibold text-slate-900 truncate">
              Documents · {projectQuery.data?.name}
            </h1>
          </div>
          <div className="flex items-center gap-4">
            <span className="text-sm text-slate-600">
              {user.name} {role && <span className="text-xs text-slate-400 uppercase">({role})</span>}
            </span>
            <button onClick={handleLogout} className="text-sm text-indigo-600 font-medium">
              Log out
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-6 py-8 space-y-6">
        {error && (
          <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-md px-4 py-2">{error}</p>
        )}

        {canEdit && (
          <div className="bg-white p-6 rounded-lg shadow space-y-3">
            <h2 className="text-base font-semibold text-slate-900">Upload a document</h2>
            <p className="text-xs text-slate-500">PDF, .txt, or .md — up to 10MB.</p>
            <div className="flex flex-col sm:flex-row gap-3 items-start sm:items-center">
              <select
                value={taskId}
                onChange={(e) => setTaskId(e.target.value)}
                className="border border-slate-300 rounded-md px-2 py-2 text-sm"
              >
                <option value="">No task (project library)</option>
                {tasksQuery.data?.map((t) => (
                  <option key={t.id} value={t.id}>
                    Attach to: {t.title}
                  </option>
                ))}
              </select>
              <input
                ref={fileInputRef}
                type="file"
                accept=".pdf,.txt,.md"
                onChange={handleFileChange}
                disabled={uploadMutation.isPending}
                className="text-sm"
              />
              {uploadMutation.isPending && <span className="text-xs text-slate-500">Uploading…</span>}
            </div>
          </div>
        )}

        <div className="bg-white p-6 rounded-lg shadow space-y-3">
          <h2 className="text-base font-semibold text-slate-900">Ask AI about this project</h2>

          {aiStatusQuery.data && !aiStatusQuery.data.configured && (
            <p className="text-xs text-slate-500 bg-slate-50 border border-slate-200 rounded-md px-3 py-2">
              AI features aren't configured for this deployment yet — everything else in CollabOS still works
              normally.
            </p>
          )}

          <form onSubmit={handleAsk} className="flex flex-col sm:flex-row gap-3">
            <input
              placeholder="e.g. What does the spec say about authentication?"
              className="flex-1 border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              disabled={aiStatusQuery.data && !aiStatusQuery.data.configured}
            />
            <button
              type="submit"
              disabled={askMutation.isPending || (aiStatusQuery.data && !aiStatusQuery.data.configured)}
              className="bg-indigo-600 text-white rounded-md px-4 py-2 text-sm font-medium hover:bg-indigo-700 disabled:opacity-50"
            >
              {askMutation.isPending ? 'Thinking…' : 'Ask'}
            </button>
          </form>

          {askResult?.configured && (
            <div className="text-sm bg-slate-50 border border-slate-200 rounded-md px-4 py-3 space-y-2">
              <p className="text-slate-900 whitespace-pre-wrap">{askResult.answer}</p>
              {askResult.sources?.length > 0 && (
                <p className="text-xs text-slate-500">Sources: {askResult.sources.join(', ')}</p>
              )}
              <AiFeedbackWidget
                key={askResult.question + askResult.answer}
                orgId={orgId}
                projectId={projectId}
                agentType="DOCUMENT_ASSISTANT"
                question={askResult.question}
                answer={askResult.answer}
              />
            </div>
          )}
        </div>

        <div className="space-y-3">
          <h2 className="text-base font-semibold text-slate-900">Files</h2>

          {documentsQuery.isLoading && <p className="text-sm text-slate-500">Loading…</p>}
          {documentsQuery.data?.length === 0 && (
            <p className="text-sm text-slate-500">No documents yet{canEdit && ' — upload the first one above'}.</p>
          )}

          <ul className="bg-white rounded-lg shadow divide-y divide-slate-100">
            {documentsQuery.data?.map((doc) => (
              <li key={doc.id} className="px-4 py-3 space-y-2">
                <div className="flex items-center justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-slate-900 truncate">{doc.originalFileName}</p>
                    <p className="text-xs text-slate-500">
                      {formatBytes(doc.fileSizeBytes)} · {doc.uploadedByName} ·{' '}
                      {new Date(doc.createdAt).toLocaleDateString()}
                      {doc.taskTitle && <> · attached to <span className="italic">{doc.taskTitle}</span></>}
                    </p>
                  </div>
                  <div className="flex items-center gap-3 shrink-0">
                    <button onClick={() => handleDownload(doc)} className="text-xs text-indigo-600 font-medium">
                      Download
                    </button>
                    <button
                      onClick={() => summarizeMutation.mutate(doc.id)}
                      disabled={summarizeMutation.isPending || (aiStatusQuery.data && !aiStatusQuery.data.configured)}
                      className="text-xs text-indigo-600 font-medium disabled:opacity-50"
                    >
                      Summarize
                    </button>
                    {(isAdmin || doc.uploadedById === user.id) && (
                      <button
                        onClick={() => deleteMutation.mutate(doc.id)}
                        disabled={deleteMutation.isPending}
                        className="text-xs text-red-600 font-medium hover:text-red-800 disabled:opacity-50"
                      >
                        Delete
                      </button>
                    )}
                  </div>
                </div>
                {summaries[doc.id]?.configured && (
                  <p className="text-xs text-slate-600 bg-slate-50 border border-slate-200 rounded-md px-3 py-2">
                    {summaries[doc.id].summary}
                  </p>
                )}
              </li>
            ))}
          </ul>
        </div>
      </main>
    </div>
  )
}
