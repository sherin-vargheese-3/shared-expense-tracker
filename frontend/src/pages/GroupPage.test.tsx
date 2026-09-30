import { screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { vi } from 'vitest'
import { AppRoutes } from '../AppRoutes'
import { alice, bob, tripGroup } from '../test/fixtures'
import { renderWithProviders } from '../test/render'
import { server } from '../test/server'

function mockGroupApi() {
  let settled = false
  const settlements: unknown[] = []
  server.use(
    http.get('/api/groups/7', () => HttpResponse.json(tripGroup)),
    http.get('/api/groups/7/expenses', () =>
      HttpResponse.json([
        {
          id: 1,
          description: 'Dinner',
          amount: 90,
          paidBy: alice,
          splitType: 'EQUAL',
          createdAt: '2026-09-02T19:30:00Z',
          splits: [
            { memberId: 1, memberName: 'Alice', amount: 30 },
            { memberId: 2, memberName: 'Bob', amount: 30 },
            { memberId: 3, memberName: 'Carol', amount: 30 },
          ],
        },
      ]),
    ),
    http.get('/api/groups/7/balances', () =>
      HttpResponse.json([
        { memberId: 1, memberName: 'Alice', balance: settled ? 30 : 60 },
        { memberId: 2, memberName: 'Bob', balance: settled ? 0 : -30 },
        { memberId: 3, memberName: 'Carol', balance: -30 },
      ]),
    ),
    http.get('/api/groups/7/suggested-payments', () =>
      HttpResponse.json(
        [
          { fromMemberId: 2, fromMemberName: 'Bob', toMemberId: 1, toMemberName: 'Alice', amount: 30 },
          { fromMemberId: 3, fromMemberName: 'Carol', toMemberId: 1, toMemberName: 'Alice', amount: 30 },
        ].slice(settled ? 1 : 0),
      ),
    ),
    http.get('/api/groups/7/settlements', () =>
      HttpResponse.json(
        settled
          ? [{ id: 1, from: bob, to: alice, amount: 30, createdAt: '2026-09-03T08:00:00Z' }]
          : [],
      ),
    ),
    http.post('/api/groups/7/settlements', async ({ request }) => {
      settlements.push(await request.json())
      settled = true
      return HttpResponse.json({ id: 1 }, { status: 201 })
    }),
  )
  return { settlements }
}

describe('GroupPage', () => {
  it('shows members, balances, suggested payments and expenses', async () => {
    mockGroupApi()
    renderWithProviders(<AppRoutes />, { route: '/groups/7' })

    expect(await screen.findByRole('heading', { level: 1, name: 'Goa trip' })).toBeInTheDocument()
    const members = screen.getByLabelText('Members')
    expect(within(members).getByText('Carol')).toBeInTheDocument()

    const balances = screen.getByRole('region', { name: 'Balances' })
    expect(await within(balances).findByText('gets back 60.00')).toBeInTheDocument()
    expect(within(balances).getAllByText('owes 30.00')).toHaveLength(2)

    const settleUp = screen.getByRole('region', { name: 'Settle up' })
    expect(await within(settleUp).findByText('Bob pays Alice')).toBeInTheDocument()
    expect(within(settleUp).getByText('Carol pays Alice')).toBeInTheDocument()

    const expenses = screen.getByRole('region', { name: 'Expenses' })
    expect(await within(expenses).findByText('Dinner')).toBeInTheDocument()
    expect(within(expenses).getByText(/Split equally: Alice 30.00, Bob 30.00, Carol 30.00/))
      .toBeInTheDocument()
    expect(within(expenses).getByText('90.00')).toBeInTheDocument()
  })

  it('records a suggested payment and refreshes balances', async () => {
    const { settlements } = mockGroupApi()
    const { user } = renderWithProviders(<AppRoutes />, { route: '/groups/7' })

    await user.click(await screen.findByRole('button', { name: 'Record: Bob pays Alice 30.00' }))

    const dialog = screen.getByRole('dialog', { name: 'Record payment' })
    expect(within(dialog).getByRole('combobox', { name: /From/ })).toHaveTextContent('Bob')
    expect(within(dialog).getByRole('combobox', { name: /To/ })).toHaveTextContent('Alice')
    expect(within(dialog).getByRole('textbox', { name: /Amount/ })).toHaveValue('30.00')

    await user.click(within(dialog).getByRole('button', { name: 'Record payment' }))

    await vi.waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(settlements).toEqual([{ fromMemberId: 2, toMemberId: 1, amount: 30 }])

    const balances = screen.getByRole('region', { name: 'Balances' })
    expect(await within(balances).findByText('settled up')).toBeInTheDocument()
    const payments = screen.getByRole('region', { name: 'Payments' })
    expect(await within(payments).findByText('Bob paid Alice')).toBeInTheDocument()
    expect(within(screen.getByRole('region', { name: 'Settle up' })).queryByText('Bob pays Alice'))
      .not.toBeInTheDocument()
  })

  it('prevents recording a payment to yourself', async () => {
    mockGroupApi()
    const { user } = renderWithProviders(<AppRoutes />, { route: '/groups/7' })

    await user.click(await screen.findByRole('button', { name: 'Record payment' }))
    const dialog = screen.getByRole('dialog')
    await user.click(within(dialog).getByRole('combobox', { name: /From/ }))
    await user.click(screen.getByRole('option', { name: 'Carol' }))
    await user.click(within(dialog).getByRole('combobox', { name: /To/ }))
    await user.click(screen.getByRole('option', { name: 'Carol' }))
    await user.type(within(dialog).getByRole('textbox', { name: /Amount/ }), '5')

    expect(within(dialog).getByText('Choose a different member')).toBeInTheDocument()
    expect(within(dialog).getByRole('button', { name: 'Record payment' })).toBeDisabled()
  })

  it('adds a member and shows duplicate-name conflicts', async () => {
    mockGroupApi()
    server.use(
      http.post('/api/groups/7/members', () =>
        HttpResponse.json(
          { status: 409, title: 'Conflict', detail: "A member named 'alice' already exists in this group" },
          { status: 409 },
        ),
      ),
    )
    const { user } = renderWithProviders(<AppRoutes />, { route: '/groups/7' })

    await user.type(await screen.findByLabelText('New member'), 'alice')
    await user.click(screen.getByRole('button', { name: 'Add member' }))

    expect(
      await screen.findByText("A member named 'alice' already exists in this group"),
    ).toBeInTheDocument()
  })

  it('shows not found for an unknown group', async () => {
    server.use(
      http.get('/api/groups/404', () =>
        HttpResponse.json({ status: 404, detail: 'Group 404 not found' }, { status: 404 }),
      ),
    )
    renderWithProviders(<AppRoutes />, { route: '/groups/404' })

    expect(await screen.findByText('Group not found.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'All groups' })).toHaveAttribute('href', '/')
  })

  it('disables adding expenses until the group has members', async () => {
    server.use(
      http.get('/api/groups/8', () =>
        HttpResponse.json({ ...tripGroup, id: 8, members: [] }),
      ),
      http.get('/api/groups/8/*', () => HttpResponse.json([])),
    )
    renderWithProviders(<AppRoutes />, { route: '/groups/8' })

    expect(await screen.findByRole('button', { name: 'Add expense' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Record payment' })).toBeDisabled()
  })

  describe('removing a member', () => {
    const settledBalances = () =>
      HttpResponse.json([
        { memberId: 1, memberName: 'Alice', balance: 30 },
        { memberId: 2, memberName: 'Bob', balance: 0 },
        { memberId: 3, memberName: 'Carol', balance: -30 },
      ])

    it('removes a settled member after confirmation', async () => {
      mockGroupApi()
      let removed: string | undefined
      server.use(
        http.get('/api/groups/7/balances', settledBalances),
        http.delete('/api/groups/7/members/:memberId', ({ params }) => {
          removed = String(params.memberId)
          return new HttpResponse(null, { status: 204 })
        }),
      )
      const { user } = renderWithProviders(<AppRoutes />, { route: '/groups/7' })

      await screen.findByText('settled up')
      await user.click(screen.getByRole('img', { name: 'Remove Bob' }))
      const dialog = screen.getByRole('dialog', { name: 'Remove Bob?' })
      expect(within(dialog).getByText(/Past expenses and payments will still show/))
        .toBeInTheDocument()
      await user.click(within(dialog).getByRole('button', { name: 'Remove' }))

      await vi.waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
      expect(removed).toBe('2')
    })

    it('explains and blocks removing a member with an open balance', async () => {
      mockGroupApi()
      const { user } = renderWithProviders(<AppRoutes />, { route: '/groups/7' })

      await screen.findByText('gets back 60.00')
      await user.click(screen.getByRole('img', { name: 'Remove Carol' }))

      const dialog = screen.getByRole('dialog', { name: 'Remove Carol?' })
      expect(within(dialog).getByText('Carol owes 30.00. Settle up before removing them.'))
        .toBeInTheDocument()
      expect(within(dialog).getByRole('button', { name: 'Remove' })).toBeDisabled()
    })

    it('shows the server error if the balance changed meanwhile', async () => {
      mockGroupApi()
      server.use(
        http.get('/api/groups/7/balances', settledBalances),
        http.delete('/api/groups/7/members/2', () =>
          HttpResponse.json(
            { status: 409, detail: 'Bob still owes 5.00. Settle up before removing them.' },
            { status: 409 },
          ),
        ),
      )
      const { user } = renderWithProviders(<AppRoutes />, { route: '/groups/7' })

      await screen.findByText('settled up')
      await user.click(screen.getByRole('img', { name: 'Remove Bob' }))
      await user.click(screen.getByRole('button', { name: 'Remove' }))

      expect(
        await screen.findByText('Bob still owes 5.00. Settle up before removing them.'),
      ).toBeInTheDocument()
      expect(screen.getByRole('dialog')).toBeInTheDocument()
    })
  })

  describe('deleting the group', () => {
    it('warns about unsettled balances, deletes, and returns to the group list', async () => {
      mockGroupApi()
      let deleted = false
      server.use(
        http.delete('/api/groups/7', () => {
          deleted = true
          return new HttpResponse(null, { status: 204 })
        }),
        http.get('/api/groups', () => HttpResponse.json([])),
      )
      const { user } = renderWithProviders(<AppRoutes />, { route: '/groups/7' })

      await screen.findByText('gets back 60.00')
      await user.click(screen.getByRole('button', { name: 'Delete group' }))
      const dialog = screen.getByRole('dialog', { name: 'Delete Goa trip?' })
      expect(within(dialog).getByText('Some balances are not settled yet.')).toBeInTheDocument()
      await user.click(within(dialog).getByRole('button', { name: 'Delete group' }))

      expect(await screen.findByText('No groups yet.')).toBeInTheDocument()
      expect(deleted).toBe(true)
    })

    it('keeps the dialog open and shows the error if deletion fails', async () => {
      mockGroupApi()
      server.use(
        http.delete('/api/groups/7', () =>
          HttpResponse.json({ status: 500, detail: 'An unexpected error occurred' }, { status: 500 }),
        ),
      )
      const { user } = renderWithProviders(<AppRoutes />, { route: '/groups/7' })

      await user.click(await screen.findByRole('button', { name: 'Delete group' }))
      await user.click(
        within(screen.getByRole('dialog')).getByRole('button', { name: 'Delete group' }),
      )

      expect(await screen.findByText('An unexpected error occurred')).toBeInTheDocument()
      expect(screen.getByRole('dialog', { name: 'Delete Goa trip?' })).toBeInTheDocument()
    })
  })
})
