import { useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import { api } from '../lib/api'
import { useAuthStore } from '../store/authStore'

const TYPING_EXPIRY_MS = 3000

// Owns the full lifecycle for "messages in this channel": fetches history over
// REST, then appends live messages arriving over the topic — including the
// current user's own sends, which come back through the same broadcast rather
// than being inserted optimistically. That keeps there being exactly one path
// messages enter the list through, instead of a local-echo path plus a
// reconciliation path.
export function useChannelSocket(orgId, channelId) {
  const token = useAuthStore((state) => state.token)
  const currentUserId = useAuthStore((state) => state.user?.id)
  const [messages, setMessages] = useState([])
  const [viewers, setViewers] = useState([])
  const [typingUsers, setTypingUsers] = useState([])
  const [connected, setConnected] = useState(false)
  const clientRef = useRef(null)
  const typingTimeouts = useRef({})

  useEffect(() => {
    setMessages([])
    setViewers([])
    setTypingUsers([])
    Object.values(typingTimeouts.current).forEach(clearTimeout)
    typingTimeouts.current = {}

    if (!token || !orgId || !channelId) return undefined

    let cancelled = false
    api.get(`/organizations/${orgId}/channels/${channelId}/messages`).then((res) => {
      if (!cancelled) setMessages(res.data)
    })

    const client = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true)

        client.subscribe(`/topic/organizations/${orgId}/channels/${channelId}/messages`, (message) => {
          setMessages((prev) => [...prev, JSON.parse(message.body)])
        })

        client.subscribe(`/topic/organizations/${orgId}/channels/${channelId}/presence`, (message) => {
          setViewers(JSON.parse(message.body))
        })

        client.subscribe(`/topic/organizations/${orgId}/channels/${channelId}/typing`, (message) => {
          const { userId, userName } = JSON.parse(message.body)
          if (userId === currentUserId) return

          setTypingUsers((prev) => (prev.some((u) => u.userId === userId) ? prev : [...prev, { userId, userName }]))

          clearTimeout(typingTimeouts.current[userId])
          typingTimeouts.current[userId] = setTimeout(() => {
            setTypingUsers((prev) => prev.filter((u) => u.userId !== userId))
          }, TYPING_EXPIRY_MS)
        })
      },
      onWebSocketClose: () => setConnected(false),
      onStompError: () => setConnected(false),
    })

    clientRef.current = client
    client.activate()

    return () => {
      cancelled = true
      client.deactivate()
      clientRef.current = null
      Object.values(typingTimeouts.current).forEach(clearTimeout)
      typingTimeouts.current = {}
    }
  }, [orgId, channelId, token, currentUserId])

  function sendTyping() {
    if (clientRef.current?.connected) {
      clientRef.current.publish({ destination: `/app/organizations/${orgId}/channels/${channelId}/typing` })
    }
  }

  return { messages, viewers, typingUsers, connected, sendTyping }
}
