import { useEffect, useState } from 'react'
import { Client } from '@stomp/stompjs'
import { useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '../store/authStore'

// Subscribing to the tasks topic just triggers a refetch of the existing
// React Query cache (built in Phase 4) rather than merging a diff — the
// server event is only ever "something changed here", so the client falls
// back on the REST endpoint it already trusts as the source of truth.
//
// Subscribing to the presence topic doubles as announcing "I'm viewing this
// board" — the backend's PresenceEventListener treats the subscribe/
// unsubscribe/disconnect itself as the join/leave signal.
export function useProjectSocket(orgId, projectId) {
  const queryClient = useQueryClient()
  const token = useAuthStore((state) => state.token)
  const [viewers, setViewers] = useState([])
  const [connected, setConnected] = useState(false)

  useEffect(() => {
    if (!token || !orgId || !projectId) return undefined

    const client = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true)

        client.subscribe(`/topic/organizations/${orgId}/projects/${projectId}/tasks`, () => {
          queryClient.invalidateQueries({ queryKey: ['tasks', orgId, projectId] })
        })

        client.subscribe(`/topic/organizations/${orgId}/projects/${projectId}/presence`, (message) => {
          setViewers(JSON.parse(message.body))
        })
      },
      onWebSocketClose: () => setConnected(false),
      onStompError: () => setConnected(false),
    })

    client.activate()
    return () => {
      client.deactivate()
    }
  }, [orgId, projectId, token, queryClient])

  return { viewers, connected }
}
