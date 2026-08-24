import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'

const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']

export default function TaskModal({ task, orgId, projectId, members, canEdit, canDelete, onClose }) {
  const queryClient = useQueryClient()
  const [title, setTitle] = useState(task.title)
  const [description, setDescription] = useState(task.description ?? '')
  const [priority, setPriority] = useState(task.priority)
  const [assigneeId, setAssigneeId] = useState(task.assigneeId ?? '')
  const [dueDate, setDueDate] = useState(task.dueDate ?? '')
  const [commentBody, setCommentBody] = useState('')
  const [error, setError] = useState('')

  const tasksKey = ['tasks', orgId, projectId]

  const commentsQuery = useQuery({
    queryKey: ['comments', orgId, projectId, task.id],
    queryFn: () =>
      api.get(`/organizations/${orgId}/projects/${projectId}/tasks/${task.id}/comments`).then((res) => res.data),
  })

  const documentsQuery = useQuery({
    queryKey: ['documents', orgId, projectId, 'task', task.id],
    queryFn: () =>
      api
        .get(`/organizations/${orgId}/projects/${projectId}/documents`, { params: { taskId: task.id } })
        .then((res) => res.data),
  })

  const updateMutation = useMutation({
    mutationFn: (payload) =>
      api.patch(`/organizations/${orgId}/projects/${projectId}/tasks/${task.id}`, payload).then((res) => res.data),
    onSuccess: () => {
      setError('')
      queryClient.invalidateQueries({ queryKey: tasksKey })
      onClose()
    },
    onError: (err) => setError(err.message),
  })

  const deleteMutation = useMutation({
    mutationFn: () => api.delete(`/organizations/${orgId}/projects/${projectId}/tasks/${task.id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: tasksKey })
      onClose()
    },
    onError: (err) => setError(err.message),
  })

  const commentMutation = useMutation({
    mutationFn: (body) =>
      api
        .post(`/organizations/${orgId}/projects/${projectId}/tasks/${task.id}/comments`, { body })
        .then((res) => res.data),
    onSuccess: () => {
      setCommentBody('')
      setError('')
      queryClient.invalidateQueries({ queryKey: ['comments', orgId, projectId, task.id] })
    },
    onError: (err) => setError(err.message),
  })

  function handleSave(e) {
    e.preventDefault()
    updateMutation.mutate({
      title,
      description,
      priority,
      assigneeId: assigneeId === '' ? null : Number(assigneeId),
      dueDate: dueDate === '' ? null : dueDate,
    })
  }

  function handleComment(e) {
    e.preventDefault()
    if (!commentBody.trim()) return
    commentMutation.mutate(commentBody.trim())
  }

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center p-4 z-50" onClick={onClose}>
      <div
        className="bg-white rounded-lg shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        <form onSubmit={handleSave} className="p-6 space-y-4">
          <div className="flex items-start justify-between gap-3">
            <input
              disabled={!canEdit}
              required
              className="flex-1 text-lg font-semibold text-slate-900 border-b border-transparent focus:border-slate-300 outline-none disabled:bg-transparent"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
            <button type="button" onClick={onClose} className="text-slate-400 hover:text-slate-600 text-xl leading-none">
              &times;
            </button>
          </div>

          <textarea
            disabled={!canEdit}
            placeholder="Description"
            rows={3}
            className="w-full border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:bg-slate-50"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />

          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs font-medium text-slate-500">Priority</label>
              <select
                disabled={!canEdit}
                value={priority}
                onChange={(e) => setPriority(e.target.value)}
                className="w-full border border-slate-300 rounded-md px-2 py-1.5 text-sm mt-1 disabled:bg-slate-50"
              >
                {PRIORITIES.map((p) => (
                  <option key={p} value={p}>
                    {p}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="text-xs font-medium text-slate-500">Assignee</label>
              <select
                disabled={!canEdit}
                value={assigneeId}
                onChange={(e) => setAssigneeId(e.target.value)}
                className="w-full border border-slate-300 rounded-md px-2 py-1.5 text-sm mt-1 disabled:bg-slate-50"
              >
                <option value="">Unassigned</option>
                {members?.map((m) => (
                  <option key={m.userId} value={m.userId}>
                    {m.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="col-span-2">
              <label className="text-xs font-medium text-slate-500">Due date</label>
              <input
                disabled={!canEdit}
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                className="w-full border border-slate-300 rounded-md px-2 py-1.5 text-sm mt-1 disabled:bg-slate-50"
              />
            </div>
          </div>

          {documentsQuery.data?.length > 0 && (
            <div>
              <label className="text-xs font-medium text-slate-500">Attachments</label>
              <ul className="mt-1 space-y-0.5">
                {documentsQuery.data.map((doc) => (
                  <li key={doc.id} className="text-sm text-indigo-700">
                    📎{' '}
                    <Link to={`/organizations/${orgId}/projects/${projectId}/documents`} className="hover:underline">
                      {doc.originalFileName}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          )}

          {error && <p className="text-sm text-red-600">{error}</p>}

          <div className="flex items-center justify-between pt-2 border-t border-slate-100">
            {canDelete ? (
              <button
                type="button"
                onClick={() => deleteMutation.mutate()}
                disabled={deleteMutation.isPending}
                className="text-sm text-red-600 font-medium hover:text-red-800 disabled:opacity-50"
              >
                Delete task
              </button>
            ) : (
              <span />
            )}
            {canEdit && (
              <button
                type="submit"
                disabled={updateMutation.isPending}
                className="bg-indigo-600 text-white rounded-md px-4 py-2 text-sm font-medium hover:bg-indigo-700 disabled:opacity-50"
              >
                {updateMutation.isPending ? 'Saving…' : 'Save changes'}
              </button>
            )}
          </div>
        </form>

        <div className="border-t border-slate-100 p-6 space-y-3">
          <h3 className="text-sm font-semibold text-slate-900">Comments</h3>

          <div className="space-y-3 max-h-48 overflow-y-auto">
            {commentsQuery.data?.length === 0 && <p className="text-sm text-slate-400">No comments yet.</p>}
            {commentsQuery.data?.map((c) => (
              <div key={c.id} className="text-sm">
                <span className="font-medium text-slate-900">{c.authorName}</span>{' '}
                <span className="text-xs text-slate-400">{new Date(c.createdAt).toLocaleString()}</span>
                <p className="text-slate-600">{c.body}</p>
              </div>
            ))}
          </div>

          {canEdit && (
            <form onSubmit={handleComment} className="flex gap-2">
              <input
                placeholder="Add a comment…"
                className="flex-1 border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                value={commentBody}
                onChange={(e) => setCommentBody(e.target.value)}
              />
              <button
                type="submit"
                disabled={commentMutation.isPending}
                className="bg-slate-800 text-white rounded-md px-3 py-2 text-sm font-medium hover:bg-slate-900 disabled:opacity-50"
              >
                Post
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  )
}
