import { describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithQueryClient } from '../test/utils'
import { api } from '../lib/api'
import AiFeedbackWidget from './AiFeedbackWidget'

vi.mock('../lib/api', () => ({
  api: { post: vi.fn() },
}))

describe('AiFeedbackWidget', () => {
  it('submits immediately on thumbs up, with no correction box', async () => {
    api.post.mockResolvedValue({})
    const user = userEvent.setup()
    renderWithQueryClient(
      <AiFeedbackWidget orgId="1" projectId="2" agentType="DOCUMENT_ASSISTANT" question="Q" answer="A" />,
    )

    await user.click(screen.getByLabelText('Thumbs up'))

    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/organizations/1/projects/2/ai/feedback', {
      agentType: 'DOCUMENT_ASSISTANT',
      question: 'Q',
      answer: 'A',
      rating: 'UP',
    }))
    expect(await screen.findByText('Thanks for the feedback.')).toBeInTheDocument()
  })

  it('shows a correction box on thumbs down and includes it on submit', async () => {
    api.post.mockResolvedValue({})
    const user = userEvent.setup()
    renderWithQueryClient(
      <AiFeedbackWidget orgId="1" projectId="2" agentType="PROJECT_MANAGER" question={null} answer="A report" />,
    )

    await user.click(screen.getByLabelText('Thumbs down'))
    expect(screen.getByPlaceholderText(/what was wrong/i)).toBeInTheDocument()

    await user.type(screen.getByPlaceholderText(/what was wrong/i), 'Missed the overdue task')
    await user.click(screen.getByRole('button', { name: 'Send' }))

    await waitFor(() => expect(api.post).toHaveBeenCalledWith('/organizations/1/projects/2/ai/feedback', {
      agentType: 'PROJECT_MANAGER',
      question: null,
      answer: 'A report',
      rating: 'DOWN',
      correction: 'Missed the overdue task',
    }))
  })
})
