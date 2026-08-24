import { describe, expect, it, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { api } from '../lib/api'
import NotificationBell from './NotificationBell'

vi.mock('../lib/api', () => ({
  api: { get: vi.fn(), post: vi.fn() },
}))

function renderBell(orgId = '1') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <NotificationBell orgId={orgId} />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('NotificationBell', () => {
  it('shows no badge when unread count is zero', async () => {
    api.get.mockResolvedValue({ data: { count: 0 } })
    renderBell()

    await waitFor(() => expect(api.get).toHaveBeenCalledWith('/organizations/1/notifications/unread-count'))
    expect(screen.queryByText(/\d/)).not.toBeInTheDocument()
  })

  it('shows the unread count badge and opens the dropdown listing notifications', async () => {
    api.get.mockImplementation((url) => {
      if (url.includes('unread-count')) return Promise.resolve({ data: { count: 2 } })
      return Promise.resolve({
        data: [
          { id: 1, type: 'TASK_CREATED', message: 'Alice created "Ship it"', link: '/x', read: false, createdAt: new Date().toISOString() },
        ],
      })
    })
    const user = userEvent.setup()
    renderBell()

    expect(await screen.findByText('2')).toBeInTheDocument()

    await user.click(screen.getByLabelText('Notifications'))

    expect(await screen.findByText('Alice created "Ship it"')).toBeInTheDocument()
  })
})
