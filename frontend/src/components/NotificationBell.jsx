import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'

export default function NotificationBell({ orgId }) {
  const [open, setOpen] = useState(false)
  const containerRef = useRef(null)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const countQuery = useQuery({
    queryKey: ['notifications-unread-count', orgId],
    queryFn: () => api.get(`/organizations/${orgId}/notifications/unread-count`).then((res) => res.data.count),
    enabled: !!orgId,
    refetchInterval: 10000,
  })

  const listQuery = useQuery({
    queryKey: ['notifications', orgId],
    queryFn: () => api.get(`/organizations/${orgId}/notifications`).then((res) => res.data),
    enabled: !!orgId && open,
  })

  const markReadMutation = useMutation({
    mutationFn: (notificationId) => api.post(`/organizations/${orgId}/notifications/${notificationId}/read`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications', orgId] })
      queryClient.invalidateQueries({ queryKey: ['notifications-unread-count', orgId] })
    },
  })

  const markAllReadMutation = useMutation({
    mutationFn: () => api.post(`/organizations/${orgId}/notifications/read-all`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications', orgId] })
      queryClient.invalidateQueries({ queryKey: ['notifications-unread-count', orgId] })
    },
  })

  useEffect(() => {
    function handleClickOutside(e) {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  function handleSelect(notification) {
    if (!notification.read) {
      markReadMutation.mutate(notification.id)
    }
    setOpen(false)
    if (notification.link) {
      navigate(notification.link)
    }
  }

  const count = countQuery.data ?? 0

  return (
    <div className="relative" ref={containerRef}>
      <button
        onClick={() => setOpen((v) => !v)}
        className="relative text-slate-500 hover:text-slate-700"
        aria-label="Notifications"
      >
        <span className="text-lg">🔔</span>
        {count > 0 && (
          <span className="absolute -top-1.5 -right-1.5 min-w-[1.1rem] h-[1.1rem] px-1 rounded-full bg-red-600 text-white text-[10px] font-semibold flex items-center justify-center">
            {count > 9 ? '9+' : count}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 mt-2 w-80 bg-white rounded-lg shadow-lg border border-slate-200 z-20">
          <div className="flex items-center justify-between px-4 py-3 border-b border-slate-100">
            <p className="text-sm font-semibold text-slate-900">Notifications</p>
            {count > 0 && (
              <button
                onClick={() => markAllReadMutation.mutate()}
                disabled={markAllReadMutation.isPending}
                className="text-xs text-indigo-600 font-medium hover:text-indigo-800 disabled:opacity-50"
              >
                Mark all read
              </button>
            )}
          </div>

          <div className="max-h-80 overflow-y-auto divide-y divide-slate-100">
            {listQuery.isLoading && <p className="px-4 py-3 text-sm text-slate-500">Loading…</p>}

            {listQuery.data?.length === 0 && (
              <p className="px-4 py-6 text-sm text-slate-400 text-center">No notifications yet</p>
            )}

            {listQuery.data?.map((notification) => (
              <button
                key={notification.id}
                onClick={() => handleSelect(notification)}
                className={`w-full text-left px-4 py-3 text-sm hover:bg-slate-50 ${
                  notification.read ? 'text-slate-500' : 'text-slate-900 bg-indigo-50/50'
                }`}
              >
                <p className={notification.read ? '' : 'font-medium'}>{notification.message}</p>
                <p className="text-xs text-slate-400 mt-1">{new Date(notification.createdAt).toLocaleString()}</p>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
