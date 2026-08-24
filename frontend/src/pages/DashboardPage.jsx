import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import { useAuthStore } from '../store/authStore'
import NotificationBell from '../components/NotificationBell'

const ROLES = ['OWNER', 'ADMIN', 'MEMBER', 'VIEWER']

export default function DashboardPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const user = useAuthStore((state) => state.user)
  const logout = useAuthStore((state) => state.logout)
  const currentOrgId = useAuthStore((state) => state.currentOrgId)
  const setCurrentOrgId = useAuthStore((state) => state.setCurrentOrgId)

  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [newOrgName, setNewOrgName] = useState('')
  const [showNewOrgForm, setShowNewOrgForm] = useState(false)
  const [inviteEmail, setInviteEmail] = useState('')
  const [inviteRole, setInviteRole] = useState('MEMBER')
  const [actionError, setActionError] = useState('')
  const [lastInviteLink, setLastInviteLink] = useState('')

  const orgsQuery = useQuery({
    queryKey: ['organizations'],
    queryFn: () => api.get('/organizations').then((res) => res.data),
  })

  // Land on a valid org: keep the persisted choice if it's still in the list,
  // otherwise fall back to the first org (every account always has at least one).
  useEffect(() => {
    if (!orgsQuery.data || orgsQuery.data.length === 0) return
    const stillValid = orgsQuery.data.some((org) => org.id === currentOrgId)
    if (!stillValid) {
      setCurrentOrgId(orgsQuery.data[0].id)
    }
  }, [orgsQuery.data, currentOrgId, setCurrentOrgId])

  const currentOrg = orgsQuery.data?.find((org) => org.id === currentOrgId)
  const role = currentOrg?.role
  const canManageUsers = role === 'OWNER' || role === 'ADMIN'
  const canChangeRoles = role === 'OWNER'

  const projectsQuery = useQuery({
    queryKey: ['projects', currentOrgId],
    queryFn: () => api.get(`/organizations/${currentOrgId}/projects`).then((res) => res.data),
    enabled: !!currentOrgId,
  })

  const membersQuery = useQuery({
    queryKey: ['members', currentOrgId],
    queryFn: () => api.get(`/organizations/${currentOrgId}/members`).then((res) => res.data),
    enabled: !!currentOrgId && canManageUsers,
  })

  const invitesQuery = useQuery({
    queryKey: ['invites', currentOrgId],
    queryFn: () => api.get(`/organizations/${currentOrgId}/invites`).then((res) => res.data),
    enabled: !!currentOrgId && canManageUsers,
  })

  const createOrgMutation = useMutation({
    mutationFn: (payload) => api.post('/organizations', payload).then((res) => res.data),
    onSuccess: (org) => {
      setNewOrgName('')
      setShowNewOrgForm(false)
      setActionError('')
      queryClient.invalidateQueries({ queryKey: ['organizations'] }).then(() => setCurrentOrgId(org.id))
    },
    onError: (err) => setActionError(err.message),
  })

  const createProjectMutation = useMutation({
    mutationFn: (payload) => api.post(`/organizations/${currentOrgId}/projects`, payload).then((res) => res.data),
    onSuccess: () => {
      setName('')
      setDescription('')
      setActionError('')
      queryClient.invalidateQueries({ queryKey: ['projects', currentOrgId] })
    },
    onError: (err) => setActionError(err.message),
  })

  const deleteProjectMutation = useMutation({
    mutationFn: (projectId) => api.delete(`/organizations/${currentOrgId}/projects/${projectId}`),
    onSuccess: () => {
      setActionError('')
      queryClient.invalidateQueries({ queryKey: ['projects', currentOrgId] })
    },
    onError: (err) => setActionError(err.message),
  })

  const roleMutation = useMutation({
    mutationFn: ({ userId, role: newRole }) =>
      api.patch(`/organizations/${currentOrgId}/members/${userId}/role`, { role: newRole }),
    onSuccess: () => {
      setActionError('')
      queryClient.invalidateQueries({ queryKey: ['members', currentOrgId] })
    },
    onError: (err) => setActionError(err.message),
  })

  const inviteMutation = useMutation({
    mutationFn: (payload) => api.post(`/organizations/${currentOrgId}/invites`, payload).then((res) => res.data),
    onSuccess: (invite) => {
      setInviteEmail('')
      setActionError('')
      setLastInviteLink(`${window.location.origin}/invite/${invite.token}`)
      queryClient.invalidateQueries({ queryKey: ['invites', currentOrgId] })
    },
    onError: (err) => setActionError(err.message),
  })

  function handleCreateOrg(e) {
    e.preventDefault()
    createOrgMutation.mutate({ name: newOrgName })
  }

  function handleCreateProject(e) {
    e.preventDefault()
    createProjectMutation.mutate({ name, description })
  }

  function handleInvite(e) {
    e.preventDefault()
    inviteMutation.mutate({ email: inviteEmail, role: inviteRole })
  }

  function handleLogout() {
    logout()
    navigate('/login')
  }

  if (orgsQuery.isLoading) {
    return <div className="min-h-screen flex items-center justify-center text-slate-500">Loading…</div>
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="bg-white border-b border-slate-200">
        <div className="max-w-4xl mx-auto px-6 py-4 flex items-center justify-between gap-4 flex-wrap">
          <h1 className="text-lg font-semibold text-slate-900">CollabOS</h1>

          <div className="flex items-center gap-3">
            <select
              value={currentOrgId ?? ''}
              onChange={(e) => setCurrentOrgId(Number(e.target.value))}
              className="text-sm border border-slate-300 rounded-md px-2 py-1"
            >
              {orgsQuery.data?.map((org) => (
                <option key={org.id} value={org.id}>
                  {org.name}
                </option>
              ))}
            </select>
            <button
              onClick={() => setShowNewOrgForm((v) => !v)}
              className="text-sm text-indigo-600 font-medium"
            >
              + New org
            </button>
          </div>

          <div className="flex items-center gap-4">
            {currentOrgId && (
              <Link to={`/organizations/${currentOrgId}/chat`} className="text-sm text-indigo-600 font-medium">
                Chat
              </Link>
            )}
            {currentOrgId && <NotificationBell orgId={currentOrgId} />}
            <span className="text-sm text-slate-600">
              {user.name} {role && <span className="text-xs text-slate-400 uppercase">({role})</span>}
            </span>
            <button onClick={handleLogout} className="text-sm text-indigo-600 font-medium">
              Log out
            </button>
          </div>
        </div>

        {showNewOrgForm && (
          <div className="max-w-4xl mx-auto px-6 pb-4">
            <form onSubmit={handleCreateOrg} className="flex gap-2">
              <input
                required
                placeholder="Organization name"
                className="flex-1 border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                value={newOrgName}
                onChange={(e) => setNewOrgName(e.target.value)}
              />
              <button
                type="submit"
                disabled={createOrgMutation.isPending}
                className="bg-indigo-600 text-white rounded-md px-4 py-2 text-sm font-medium hover:bg-indigo-700 disabled:opacity-50"
              >
                Create
              </button>
            </form>
          </div>
        )}
      </header>

      <main className="max-w-4xl mx-auto px-6 py-8 space-y-8">
        {actionError && (
          <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-md px-4 py-2">{actionError}</p>
        )}

        {role !== 'VIEWER' && (
          <form onSubmit={handleCreateProject} className="bg-white p-6 rounded-lg shadow space-y-4">
            <h2 className="text-base font-semibold text-slate-900">New project</h2>
            <div className="flex flex-col sm:flex-row gap-3">
              <input
                required
                placeholder="Project name"
                className="flex-1 border border-slate-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
              <input
                placeholder="Description (optional)"
                className="flex-1 border border-slate-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
              <button
                type="submit"
                disabled={createProjectMutation.isPending}
                className="bg-indigo-600 text-white rounded-md px-4 py-2 font-medium hover:bg-indigo-700 disabled:opacity-50"
              >
                {createProjectMutation.isPending ? 'Creating…' : 'Create'}
              </button>
            </div>
          </form>
        )}

        <div className="space-y-3">
          <h2 className="text-base font-semibold text-slate-900">Projects in {currentOrg?.name}</h2>

          {projectsQuery.isLoading && <p className="text-sm text-slate-500">Loading projects…</p>}

          {projectsQuery.data?.length === 0 && (
            <p className="text-sm text-slate-500">No projects yet{role !== 'VIEWER' && ' — create your first one above'}.</p>
          )}

          <ul className="grid gap-3 sm:grid-cols-2">
            {projectsQuery.data?.map((project) => (
              <li key={project.id} className="bg-white p-4 rounded-lg shadow flex justify-between gap-2">
                <Link to={`/organizations/${currentOrgId}/projects/${project.id}`} className="min-w-0">
                  <p className="font-medium text-slate-900 hover:text-indigo-600">{project.name}</p>
                  {project.description && <p className="text-sm text-slate-600 mt-1">{project.description}</p>}
                  <p className="text-xs text-slate-400 mt-2">
                    By {project.ownerName} · {new Date(project.createdAt).toLocaleDateString()}
                  </p>
                </Link>
                {(role === 'OWNER' || role === 'ADMIN' || project.ownerId === user.id) && role !== 'VIEWER' && (
                  <button
                    onClick={() => deleteProjectMutation.mutate(project.id)}
                    disabled={deleteProjectMutation.isPending}
                    className="text-xs text-red-600 font-medium hover:text-red-800 disabled:opacity-50 h-fit"
                  >
                    Delete
                  </button>
                )}
              </li>
            ))}
          </ul>
        </div>

        {canManageUsers && (
          <div className="space-y-3">
            <h2 className="text-base font-semibold text-slate-900">Team members</h2>
            <div className="bg-white rounded-lg shadow divide-y divide-slate-100">
              {membersQuery.data?.map((member) => (
                <div key={member.userId} className="flex items-center justify-between px-4 py-3">
                  <div>
                    <p className="text-sm font-medium text-slate-900">{member.name}</p>
                    <p className="text-xs text-slate-500">{member.email}</p>
                  </div>
                  {canChangeRoles ? (
                    <select
                      value={member.role}
                      onChange={(e) => roleMutation.mutate({ userId: member.userId, role: e.target.value })}
                      className="text-sm border border-slate-300 rounded-md px-2 py-1"
                    >
                      {ROLES.map((r) => (
                        <option key={r} value={r}>
                          {r}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <span className="text-xs text-slate-400 uppercase">{member.role}</span>
                  )}
                </div>
              ))}
            </div>

            <form onSubmit={handleInvite} className="bg-white p-6 rounded-lg shadow space-y-3">
              <h3 className="text-sm font-semibold text-slate-900">Invite someone</h3>
              <div className="flex flex-col sm:flex-row gap-3">
                <input
                  required
                  type="email"
                  placeholder="email@example.com"
                  className="flex-1 border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  value={inviteEmail}
                  onChange={(e) => setInviteEmail(e.target.value)}
                />
                <select
                  value={inviteRole}
                  onChange={(e) => setInviteRole(e.target.value)}
                  className="border border-slate-300 rounded-md px-2 py-2 text-sm"
                >
                  {ROLES.map((r) => (
                    <option key={r} value={r}>
                      {r}
                    </option>
                  ))}
                </select>
                <button
                  type="submit"
                  disabled={inviteMutation.isPending}
                  className="bg-indigo-600 text-white rounded-md px-4 py-2 text-sm font-medium hover:bg-indigo-700 disabled:opacity-50"
                >
                  Create invite link
                </button>
              </div>

              {lastInviteLink && (
                <div className="text-sm bg-slate-50 border border-slate-200 rounded-md px-3 py-2">
                  <p className="text-slate-500 mb-1">Share this link with them (no email is sent):</p>
                  <input
                    readOnly
                    value={lastInviteLink}
                    onFocus={(e) => e.target.select()}
                    className="w-full bg-transparent text-indigo-700 text-xs outline-none"
                  />
                </div>
              )}

              {invitesQuery.data?.length > 0 && (
                <div className="pt-2 space-y-1">
                  <p className="text-xs font-medium text-slate-500">Pending invites</p>
                  {invitesQuery.data.map((invite) => (
                    <p key={invite.id} className="text-xs text-slate-500">
                      {invite.email} · {invite.role}
                    </p>
                  ))}
                </div>
              )}
            </form>
          </div>
        )}
      </main>
    </div>
  )
}
