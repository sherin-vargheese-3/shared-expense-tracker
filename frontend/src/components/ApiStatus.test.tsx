import { render, screen } from '@testing-library/react'
import { vi } from 'vitest'
import { ApiStatus } from './ApiStatus'

const mockFetch = (response: Partial<Response>) =>
  vi.spyOn(globalThis, 'fetch').mockResolvedValue(response as Response)

describe('ApiStatus', () => {
  it('shows online when the backend reports UP', async () => {
    mockFetch({ ok: true, status: 200, json: async () => ({ status: 'UP' }) })

    render(<ApiStatus />)

    expect(await screen.findByText('API: online')).toBeInTheDocument()
    expect(fetch).toHaveBeenCalledWith('/actuator/health', expect.anything())
  })

  it('shows offline when the backend is unreachable', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('Failed to fetch'))

    render(<ApiStatus />)

    expect(await screen.findByText('API: offline')).toBeInTheDocument()
  })

  it('shows offline when the backend returns an error status', async () => {
    mockFetch({ ok: false, status: 503, json: async () => ({ status: 'DOWN' }) })

    render(<ApiStatus />)

    expect(await screen.findByText('API: offline')).toBeInTheDocument()
  })
})
