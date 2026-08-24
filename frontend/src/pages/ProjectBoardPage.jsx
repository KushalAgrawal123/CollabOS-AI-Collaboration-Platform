import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import { useAuthStore } from '../store/authStore'
import { useProjectSocket } from '../hooks/useProjectSocket'
import TaskModal from '../components/TaskModal'
import NotificationBell from '../components/NotificationBell'
import AiFeedbackWidget from '../components/AiFeedbackWidget'

const COLUMNS = [
  { key: 'TODO', label: 'To Do' },
  { key: 'IN_PROGRESS', label: 'In Progress' },
  { key: 'DONE', label: 'Completed' },
]

const PRIORITY_STYLES = {
  LOW: 'bg-slate-100 text-slate-600',
  MEDIUM: 'bg-blue-100 text-blue-700',
  HIGH: 'bg-amber-100 text-amber-700',
  URGENT: 'bg-red-100 text-red-700',
}

export default function ProjectBoardPage() {
  const { orgId, projectId } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const user = useAuthStore((state) => state.user)
  const logout = useAuthStore((state) => state.logout)
  const setCurrentOrgId = useAuthStore((state) => state.setCurrentOrgId)

  const [quickAddTitle, setQuickAddTitle] = useState('')
  const [openTaskId, setOpenTaskId] = useState(null)
  const [dragTaskId, setDragTaskId] = useState(null)
  const [dropTarget, setDropTarget] = useState(null) // { status, index } — mirrors dropTargetRef, for rendering only
  const [error, setError] = useState('')
  const [showAiSummary, setShowAiSummary] = useState(false)

  // dragover and drop can fire within the same React batch, so handleDrop reading
  // the `dropTarget` state closure can see a stale (pre-dragover) value. A ref is
  // updated synchronously alongside the state and is what handleDrop actually reads.
  const dragTaskIdRef = useRef(null)
  const dropTargetRef = useRef(null)

  useEffect(() => {
    setCurrentOrgId(Number(orgId))
  }, [orgId, setCurrentOrgId])

  const { viewers, connected } = useProjectSocket(orgId, projectId)

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

  const membersQuery = useQuery({
    queryKey: ['members', orgId],
    queryFn: () => api.get(`/organizations/${orgId}/members`).then((res) => res.data),
  })

  const columns = useMemo(() => {
    const grouped = { TODO: [], IN_PROGRESS: [], DONE: [] }
    for (const task of tasksQuery.data ?? []) {
      grouped[task.status].push(task)
    }
    for (const key of Object.keys(grouped)) {
      grouped[key].sort((a, b) => a.position - b.position)
    }
    return grouped
  }, [tasksQuery.data])

  const aiStatusQuery = useQuery({
    queryKey: ['ai-status', orgId],
    queryFn: () => api.get(`/organizations/${orgId}/ai/status`).then((res) => res.data),
  })

  const projectManagerMutation = useMutation({
    mutationFn: () =>
      api.post(`/organizations/${orgId}/projects/${projectId}/ai/project-manager`).then((res) => res.data),
    onError: (err) => setError(err.message),
  })

  const createMutation = useMutation({
    mutationFn: (title) =>
      api.post(`/organizations/${orgId}/projects/${projectId}/tasks`, { title }).then((res) => res.data),
    onSuccess: () => {
      setQuickAddTitle('')
      setError('')
      queryClient.invalidateQueries({ queryKey: ['tasks', orgId, projectId] })
    },
    onError: (err) => setError(err.message),
  })

  const reorderMutation = useMutation({
    mutationFn: ({ status, taskIds }) =>
      api.patch(`/organizations/${orgId}/projects/${projectId}/tasks/reorder`, { status, taskIds }),
    onSuccess: () => {
      setError('')
      queryClient.invalidateQueries({ queryKey: ['tasks', orgId, projectId] })
    },
    onError: (err) => {
      setError(err.message)
      queryClient.invalidateQueries({ queryKey: ['tasks', orgId, projectId] })
    },
  })

  function handleQuickAdd(e) {
    e.preventDefault()
    if (!quickAddTitle.trim()) return
    createMutation.mutate(quickAddTitle.trim())
  }

  function handleDragStart(taskId) {
    dragTaskIdRef.current = taskId
    setDragTaskId(taskId)
  }

  function handleCardDragOver(e, status, index) {
    e.preventDefault()
    // Stop this from also bubbling to the column's onDragOver — otherwise the
    // column handler would immediately overwrite this precise position with
    // "append at end" on every mouse move over a card.
    e.stopPropagation()
    const rect = e.currentTarget.getBoundingClientRect()
    const isAfter = e.clientY - rect.top > rect.height / 2
    const next = { status, index: isAfter ? index + 1 : index }
    dropTargetRef.current = next
    setDropTarget(next)
  }

  function handleColumnDragOver(e, status) {
    e.preventDefault()
    // Only reached for background hovers now that card hovers stop propagation —
    // no need to compare e.target/currentTarget (nested wrapper divs made that unreliable).
    const next = { status, index: columns[status].length }
    dropTargetRef.current = next
    setDropTarget(next)
  }

  function handleDrop(e) {
    e.preventDefault()
    const taskId = dragTaskIdRef.current
    const target = dropTargetRef.current
    if (taskId == null || target == null) return

    const { status, index } = target
    const currentIds = columns[status].map((t) => t.id).filter((id) => id !== taskId)
    currentIds.splice(Math.min(index, currentIds.length), 0, taskId)

    reorderMutation.mutate({ status, taskIds: currentIds })
    dragTaskIdRef.current = null
    dropTargetRef.current = null
    setDragTaskId(null)
    setDropTarget(null)
  }

  function handleDragEnd() {
    dragTaskIdRef.current = null
    dropTargetRef.current = null
    setDragTaskId(null)
    setDropTarget(null)
  }

  function handleLogout() {
    logout()
    navigate('/login')
  }

  const openTask = tasksQuery.data?.find((t) => t.id === openTaskId)

  if (projectQuery.isLoading) {
    return <div className="min-h-screen flex items-center justify-center text-slate-500">Loading…</div>
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="bg-white border-b border-slate-200">
        <div className="max-w-6xl mx-auto px-6 py-4 flex items-center justify-between gap-4 flex-wrap">
          <div className="flex items-center gap-3 min-w-0">
            <Link to="/dashboard" className="text-sm text-indigo-600 font-medium shrink-0">
              ← Dashboard
            </Link>
            <h1 className="text-lg font-semibold text-slate-900 truncate">{projectQuery.data?.name}</h1>
          </div>
          <div className="flex items-center gap-4">
            <Link to={`/organizations/${orgId}/projects/${projectId}/documents`} className="text-sm text-indigo-600 font-medium shrink-0">
              Documents
            </Link>
            {viewers.length > 0 && (
              <div className="flex items-center gap-1.5" title={viewers.map((v) => v.userName).join(', ')}>
                <span className={`inline-block w-1.5 h-1.5 rounded-full ${connected ? 'bg-green-500' : 'bg-slate-300'}`} />
                <div className="flex -space-x-2">
                  {viewers.slice(0, 5).map((v) => (
                    <span
                      key={v.userId}
                      className="w-6 h-6 rounded-full bg-indigo-100 text-indigo-700 text-[10px] font-semibold flex items-center justify-center border-2 border-white"
                    >
                      {v.userName.slice(0, 1).toUpperCase()}
                    </span>
                  ))}
                </div>
                <span className="text-xs text-slate-500">
                  {viewers.length} viewing
                </span>
              </div>
            )}
            <NotificationBell orgId={orgId} />
            <span className="text-sm text-slate-600">
              {user.name} {role && <span className="text-xs text-slate-400 uppercase">({role})</span>}
            </span>
            <button onClick={handleLogout} className="text-sm text-indigo-600 font-medium">
              Log out
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-6 py-8">
        {error && (
          <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-md px-4 py-2 mb-4">{error}</p>
        )}

        {aiStatusQuery.data?.configured && (
          <div className="bg-white p-4 rounded-lg shadow mb-4">
            <div className="flex items-center justify-between gap-3">
              <h2 className="text-sm font-semibold text-slate-900">AI Project Summary</h2>
              <button
                onClick={() => {
                  setShowAiSummary(true)
                  projectManagerMutation.mutate()
                }}
                disabled={projectManagerMutation.isPending}
                className="text-xs text-indigo-600 font-medium disabled:opacity-50"
              >
                {projectManagerMutation.isPending ? 'Analyzing…' : 'Get AI summary'}
              </button>
            </div>

            {showAiSummary && projectManagerMutation.data?.configured && (
              <div className="mt-3 text-sm bg-slate-50 border border-slate-200 rounded-md px-4 py-3">
                <p className="text-slate-900 whitespace-pre-wrap">{projectManagerMutation.data.report}</p>
                <AiFeedbackWidget
                  key={projectManagerMutation.data.report}
                  orgId={orgId}
                  projectId={projectId}
                  agentType="PROJECT_MANAGER"
                  question={null}
                  answer={projectManagerMutation.data.report}
                />
              </div>
            )}
          </div>
        )}

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          {COLUMNS.map(({ key, label }) => (
            <div
              key={key}
              className="bg-slate-100 rounded-lg p-3 flex flex-col min-h-[200px]"
              onDragOver={(e) => handleColumnDragOver(e, key)}
              onDrop={handleDrop}
            >
              <h2 className="text-sm font-semibold text-slate-700 mb-3 px-1">
                {label} <span className="text-slate-400 font-normal">({columns[key].length})</span>
              </h2>

              <div className="space-y-2 flex-1">
                {columns[key].map((task, index) => (
                  <div key={task.id}>
                    {dropTarget?.status === key && dropTarget.index === index && dragTaskId != null && (
                      <div className="h-1 bg-indigo-400 rounded-full mb-2" />
                    )}
                    <div
                      draggable={canEdit}
                      onDragStart={() => handleDragStart(task.id)}
                      onDragOver={(e) => handleCardDragOver(e, key, index)}
                      onDragEnd={handleDragEnd}
                      onClick={() => setOpenTaskId(task.id)}
                      className={`bg-white rounded-md shadow p-3 cursor-pointer hover:shadow-md transition ${
                        dragTaskId === task.id ? 'opacity-40' : ''
                      }`}
                    >
                      <p className="text-sm font-medium text-slate-900">{task.title}</p>
                      <div className="flex items-center justify-between mt-2">
                        <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded ${PRIORITY_STYLES[task.priority]}`}>
                          {task.priority}
                        </span>
                        {task.assigneeName && (
                          <span className="text-[10px] text-slate-500 truncate ml-2">{task.assigneeName}</span>
                        )}
                      </div>
                    </div>
                  </div>
                ))}

                {dropTarget?.status === key && dropTarget.index === columns[key].length && dragTaskId != null && (
                  <div className="h-1 bg-indigo-400 rounded-full" />
                )}
              </div>

              {key === 'TODO' && canEdit && (
                <form onSubmit={handleQuickAdd} className="mt-3">
                  <input
                    placeholder="+ Add a task"
                    className="w-full bg-white border border-slate-200 rounded-md px-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    value={quickAddTitle}
                    onChange={(e) => setQuickAddTitle(e.target.value)}
                  />
                </form>
              )}
            </div>
          ))}
        </div>
      </main>

      {openTask && (
        <TaskModal
          task={openTask}
          orgId={orgId}
          projectId={projectId}
          members={membersQuery.data}
          canEdit={canEdit}
          canDelete={isAdmin || openTask.createdById === user.id}
          onClose={() => setOpenTaskId(null)}
        />
      )}
    </div>
  )
}
