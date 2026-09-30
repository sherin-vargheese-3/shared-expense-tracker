import { screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { AppRoutes } from '../AppRoutes'
import { server } from '../test/server'
import { renderWithProviders } from '../test/render'
import { tripGroup, validationProblem } from '../test/fixtures'

describe('GroupsPage', () => {
  it('lists groups with member counts', async () => {
    server.use(
      http.get('/api/groups', () =>
        HttpResponse.json([
          { id: 1, name: 'Flat', memberCount: 1, createdAt: '2026-09-01T10:00:00Z' },
          { id: 2, name: 'Goa trip', memberCount: 3, createdAt: '2026-09-02T10:00:00Z' },
        ]),
      ),
    )

    renderWithProviders(<AppRoutes />)

    expect(await screen.findByRole('heading', { name: 'Flat' })).toBeInTheDocument()
    expect(screen.getByText(/1 member ·/)).toBeInTheDocument()
    expect(screen.getByText(/3 members ·/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Goa trip/ })).toHaveAttribute('href', '/groups/2')
  })

  it('shows an empty state', async () => {
    server.use(http.get('/api/groups', () => HttpResponse.json([])))

    renderWithProviders(<AppRoutes />)

    expect(await screen.findByText('No groups yet.')).toBeInTheDocument()
  })

  it('shows an error with retry when the API fails', async () => {
    server.use(
      http.get('/api/groups', () =>
        HttpResponse.json({ status: 500, detail: 'An unexpected error occurred' }, { status: 500 }),
      ),
    )

    renderWithProviders(<AppRoutes />)

    expect(await screen.findByText('An unexpected error occurred')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument()
  })

  it('creates a group with members and opens it', async () => {
    let body: unknown
    server.use(
      http.get('/api/groups', () => HttpResponse.json([])),
      http.post('/api/groups', async ({ request }) => {
        body = await request.json()
        return HttpResponse.json(tripGroup, { status: 201 })
      }),
      http.get('/api/groups/7', () => HttpResponse.json(tripGroup)),
      http.get('/api/groups/7/*', () => HttpResponse.json([])),
    )
    const { user } = renderWithProviders(<AppRoutes />)

    await user.click(await screen.findByRole('button', { name: 'New group' }))
    const dialog = screen.getByRole('dialog')
    await user.type(within(dialog).getByLabelText(/Group name/), '  Goa trip ')
    await user.type(within(dialog).getByLabelText('Add member'), 'Alice{Enter}')
    await user.type(within(dialog).getByLabelText('Add member'), 'alice{Enter}')
    expect(within(dialog).getByText('alice is already in the list')).toBeInTheDocument()
    await user.clear(within(dialog).getByLabelText('Add member'))
    await user.type(within(dialog).getByLabelText('Add member'), 'Bob{Enter}')
    // A typed-but-not-added name is still included on submit.
    await user.type(within(dialog).getByLabelText('Add member'), 'Carol')
    await user.click(within(dialog).getByRole('button', { name: 'Create group' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Goa trip' })).toBeInTheDocument()
    expect(body).toEqual({ name: 'Goa trip', members: ['Alice', 'Bob', 'Carol'] })
  })

  it('shows server validation errors on the field', async () => {
    server.use(
      http.get('/api/groups', () => HttpResponse.json([])),
      http.post('/api/groups', () =>
        HttpResponse.json(
          validationProblem([{ field: 'name', message: 'size must be between 0 and 100' }]),
          { status: 400 },
        ),
      ),
    )
    const { user } = renderWithProviders(<AppRoutes />)

    await user.click(await screen.findByRole('button', { name: 'New group' }))
    await user.type(screen.getByLabelText(/Group name/), 'X')
    await user.click(screen.getByRole('button', { name: 'Create group' }))

    expect(await screen.findByText('size must be between 0 and 100')).toBeInTheDocument()
    expect(screen.getByRole('dialog')).toBeInTheDocument()
  })
})
