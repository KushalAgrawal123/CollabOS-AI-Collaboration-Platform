import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import { useAuthStore } from '../store/authStore'
import { useChannelSocket } from '../hooks/useChannelSocket'

export default function ChatPage() {
  const { orgId } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const user = useAuthStore((state) => state.user)
  const logout = useAuthStore((state) => state.logout)
  const setCurrentOrgId = useAuthStore((state) => state.setCurrentOrgId)

  const [selectedChannelId, setSelectedChannelId] = useState(null)
  const [showNewChannelForm, setShowNewChannelForm] = useState(false)
  const [showMemberPicker, setShowMemberPicker] = useState(false)
  const [newChannelName, setNewChannelName] = useState('')
  const [messageBody, setMessageBody] = useState('')
  const [error, setError] = useState('')
  const messagesEndRef = useRef(null)

  useEffect(() => {
    setCurrentOrgId(Number(orgId))
  }, [orgId, setCurrentOrgId])

  const orgsQuery = useQuery({
    queryKey: ['organizations'],
    queryFn: () => api.get('/organizations').then((res) => res.data),
  })
  const role = orgsQuery.data?.find((o) => o.id === Number(orgId))?.role
  const canEdit = role && role !== 'VIEWER'

  const channelsQuery = useQuery({
    queryKey: ['channels', orgId],
    queryFn: () => api.get(`/organizations/${orgId}/channels`).then((res) => res.data),
    enabled: !!orgId,
  })

  const membersQuery = useQuery({
    queryKey: ['members', orgId],
    queryFn: () => api.get(`/organizations/${orgId}/members`).then((res) => res.data),
    enabled: showMemberPicker,
  })

  // Land on a channel automatically once the list loads, so chat isn't a blank page.
  useEffect(() => {
    if (selectedChannelId == null && channelsQuery.data?.length > 0) {
      setSelectedChannelId(channelsQuery.data[0].id)
    }
  }, [channelsQuery.data, selectedChannelId])

  const { messages, viewers, typingUsers, connected, sendTyping } = useChannelSocket(orgId, selectedChannelId)

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const createChannelMutation = useMutation({
    mutationFn: (name) => api.post(`/organizations/${orgId}/channels`, { name }).then((res) => res.data),
    onSuccess: (channel) => {
      setNewChannelName('')
      setShowNewChannelForm(false)
      setError('')
      queryClient.invalidateQueries({ queryKey: ['channels', orgId] }).then(() => setSelectedChannelId(channel.id))
    },
    onError: (err) => setError(err.message),
  })

  const openDirectMutation = useMutation({
    mutationFn: (userId) => api.post(`/organizations/${orgId}/channels/direct`, { userId }).then((res) => res.data),
    onSuccess: (channel) => {
      setShowMemberPicker(false)
      setError('')
      queryClient.invalidateQueries({ queryKey: ['channels', orgId] }).then(() => setSelectedChannelId(channel.id))
    },
    onError: (err) => setError(err.message),
  })

  const sendMessageMutation = useMutation({
    mutationFn: (body) =>
      api.post(`/organizations/${orgId}/channels/${selectedChannelId}/messages`, { body }),
    onSuccess: () => {
      setMessageBody('')
      setError('')
    },
    onError: (err) => setError(err.message),
  })

  function handleCreateChannel(e) {
    e.preventDefault()
    if (!newChannelName.trim()) return
    createChannelMutation.mutate(newChannelName.trim())
  }

  function handleSend(e) {
    e.preventDefault()
    if (!messageBody.trim()) return
    sendMessageMutation.mutate(messageBody.trim())
  }

  function handleLogout() {
    logout()
    navigate('/login')
  }

  const publicChannels = channelsQuery.data?.filter((c) => c.type === 'PUBLIC') ?? []
  const directChannels = channelsQuery.data?.filter((c) => c.type === 'DIRECT') ?? []
  const selectedChannel = channelsQuery.data?.find((c) => c.id === selectedChannelId)

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <header className="bg-white border-b border-slate-200">
        <div className="max-w-6xl mx-auto px-6 py-4 flex items-center justify-between gap-4 flex-wrap">
          <div className="flex items-center gap-3">
            <Link to="/dashboard" className="text-sm text-indigo-600 font-medium shrink-0">
              ← Dashboard
            </Link>
            <h1 className="text-lg font-semibold text-slate-900">Chat</h1>
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

      {error && (
        <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-md px-4 py-2 max-w-6xl mx-auto mt-4 w-full">
          {error}
        </p>
      )}

      <main className="flex-1 max-w-6xl mx-auto w-full px-6 py-6 grid grid-cols-[220px_1fr] gap-4 min-h-0">
        <aside className="bg-white rounded-lg shadow p-3 flex flex-col gap-4 overflow-y-auto">
          <div>
            <div className="flex items-center justify-between mb-1">
              <p className="text-xs font-semibold text-slate-500 uppercase">Channels</p>
              {canEdit && (
                <button onClick={() => setShowNewChannelForm((v) => !v)} className="text-xs text-indigo-600 font-medium">
                  + New
                </button>
              )}
            </div>
            {showNewChannelForm && (
              <form onSubmit={handleCreateChannel} className="mb-2">
                <input
                  autoFocus
                  placeholder="channel-name"
                  className="w-full text-sm border border-slate-300 rounded-md px-2 py-1"
                  value={newChannelName}
                  onChange={(e) => setNewChannelName(e.target.value)}
                />
              </form>
            )}
            <ul className="space-y-0.5">
              {publicChannels.map((c) => (
                <li key={c.id}>
                  <button
                    onClick={() => setSelectedChannelId(c.id)}
                    className={`w-full text-left text-sm px-2 py-1 rounded ${
                      c.id === selectedChannelId ? 'bg-indigo-50 text-indigo-700 font-medium' : 'text-slate-700 hover:bg-slate-50'
                    }`}
                  >
                    # {c.displayName}
                  </button>
                </li>
              ))}
            </ul>
          </div>

          <div>
            <div className="flex items-center justify-between mb-1">
              <p className="text-xs font-semibold text-slate-500 uppercase">Direct Messages</p>
              {canEdit && (
                <button onClick={() => setShowMemberPicker((v) => !v)} className="text-xs text-indigo-600 font-medium">
                  + New
                </button>
              )}
            </div>
            {showMemberPicker && (
              <ul className="mb-2 border border-slate-200 rounded-md divide-y divide-slate-100 max-h-40 overflow-y-auto">
                {membersQuery.data
                  ?.filter((m) => m.userId !== user.id)
                  .map((m) => (
                    <li key={m.userId}>
                      <button
                        onClick={() => openDirectMutation.mutate(m.userId)}
                        className="w-full text-left text-sm px-2 py-1 hover:bg-slate-50"
                      >
                        {m.name}
                      </button>
                    </li>
                  ))}
              </ul>
            )}
            <ul className="space-y-0.5">
              {directChannels.map((c) => (
                <li key={c.id}>
                  <button
                    onClick={() => setSelectedChannelId(c.id)}
                    className={`w-full text-left text-sm px-2 py-1 rounded ${
                      c.id === selectedChannelId ? 'bg-indigo-50 text-indigo-700 font-medium' : 'text-slate-700 hover:bg-slate-50'
                    }`}
                  >
                    {c.displayName}
                  </button>
                </li>
              ))}
            </ul>
          </div>
        </aside>

        <section className="bg-white rounded-lg shadow flex flex-col min-h-0">
          {selectedChannel ? (
            <>
              <div className="border-b border-slate-100 px-4 py-3 flex items-center justify-between">
                <h2 className="text-sm font-semibold text-slate-900">
                  {selectedChannel.type === 'PUBLIC' ? `# ${selectedChannel.displayName}` : selectedChannel.displayName}
                </h2>
                {viewers.length > 0 && (
                  <div className="flex items-center gap-1.5" title={viewers.map((v) => v.userName).join(', ')}>
                    <span className={`inline-block w-1.5 h-1.5 rounded-full ${connected ? 'bg-green-500' : 'bg-slate-300'}`} />
                    <span className="text-xs text-slate-400">{viewers.length} here</span>
                  </div>
                )}
              </div>

              <div className="flex-1 overflow-y-auto px-4 py-3 space-y-3">
                {messages.map((m) => (
                  <div key={m.id}>
                    <span className="text-sm font-medium text-slate-900">{m.authorName}</span>{' '}
                    <span className="text-xs text-slate-400">{new Date(m.createdAt).toLocaleTimeString()}</span>
                    <p className="text-sm text-slate-700">{m.body}</p>
                  </div>
                ))}
                <div ref={messagesEndRef} />
              </div>

              <div className="px-4 py-1 h-5 text-xs text-slate-400 italic">
                {typingUsers.length > 0 &&
                  `${typingUsers.map((u) => u.userName).join(', ')} ${typingUsers.length === 1 ? 'is' : 'are'} typing…`}
              </div>

              {canEdit ? (
                <form onSubmit={handleSend} className="border-t border-slate-100 p-3 flex gap-2">
                  <input
                    placeholder="Message…"
                    className="flex-1 border border-slate-300 rounded-md px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    value={messageBody}
                    onChange={(e) => {
                      setMessageBody(e.target.value)
                      sendTyping()
                    }}
                  />
                  <button
                    type="submit"
                    disabled={sendMessageMutation.isPending}
                    className="bg-indigo-600 text-white rounded-md px-4 py-2 text-sm font-medium hover:bg-indigo-700 disabled:opacity-50"
                  >
                    Send
                  </button>
                </form>
              ) : (
                <div className="border-t border-slate-100 p-3 text-xs text-slate-400">Viewers can't send messages.</div>
              )}
            </>
          ) : (
            <div className="flex-1 flex items-center justify-center text-sm text-slate-400">
              {channelsQuery.isLoading ? 'Loading…' : 'No channels yet — create one to get started.'}
            </div>
          )}
        </section>
      </main>
    </div>
  )
}
