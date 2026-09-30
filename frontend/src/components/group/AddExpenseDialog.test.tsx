import { screen, within } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { vi } from 'vitest'
import { alice, bob, carol, validationProblem } from '../../test/fixtures'
import { renderWithProviders } from '../../test/render'
import { server } from '../../test/server'
import { AddExpenseDialog } from './AddExpenseDialog'

const members = [alice, bob, carol]

function setup() {
  const requests: unknown[] = []
  server.use(
    http.post('/api/groups/7/expenses', async ({ request }) => {
      requests.push(await request.json())
      return HttpResponse.json({ id: 1 }, { status: 201 })
    }),
  )
  const onClose = vi.fn()
  const utils = renderWithProviders(
    <AddExpenseDialog groupId={7} members={members} onClose={onClose} />,
  )
  const submit = () => screen.getByRole('button', { name: 'Add expense' })
  return { ...utils, requests, onClose, submit }
}

describe('AddExpenseDialog', () => {
  it('previews equal shares with the same rounding as the backend and submits them', async () => {
    const { user, requests, onClose, submit } = setup()

    await user.type(screen.getByLabelText(/Description/), 'Dinner')
    await user.type(screen.getByLabelText(/Amount/), '100')

    expect(screen.getByText('33.34')).toBeInTheDocument()
    expect(screen.getAllByText('33.33')).toHaveLength(2)

    await user.click(screen.getByRole('checkbox', { name: 'Carol' }))
    expect(screen.getAllByText('50.00')).toHaveLength(2)

    await user.click(submit())

    await vi.waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(requests).toEqual([
      {
        description: 'Dinner',
        amount: 100,
        paidBy: 1,
        splitType: 'EQUAL',
        splits: [{ memberId: 1 }, { memberId: 2 }],
      },
    ])
  })

  it('tracks the remaining amount for exact splits and only enables submit when it is zero', async () => {
    const { user, requests, submit } = setup()

    await user.type(screen.getByLabelText(/Description/), 'Hotel')
    await user.type(screen.getByLabelText(/Amount/), '250.50')
    await user.click(screen.getByRole('button', { name: 'Exact amounts' }))

    const status = screen.getByRole('status')
    expect(status).toHaveTextContent('Remaining: 250.50')

    await user.type(screen.getByRole('textbox', { name: 'Alice' }), '100.25')
    expect(status).toHaveTextContent('Remaining: 150.25')
    expect(submit()).toBeDisabled()

    await user.type(screen.getByRole('textbox', { name: 'Carol' }), '200')
    expect(status).toHaveTextContent('Over by 49.75')
    expect(submit()).toBeDisabled()

    await user.clear(screen.getByRole('textbox', { name: 'Carol' }))
    await user.type(screen.getByRole('textbox', { name: 'Carol' }), '150.25')
    expect(status).toHaveTextContent('Fully allocated')
    expect(submit()).toBeEnabled()

    await user.click(submit())

    await vi.waitFor(() => expect(requests).toHaveLength(1))
    expect(requests[0]).toMatchObject({
      splitType: 'EXACT',
      amount: 250.5,
      splits: [
        { memberId: 1, amount: 100.25 },
        { memberId: 3, amount: 150.25 },
      ],
    })
  })

  it('flags invalid amounts and keeps submit disabled', async () => {
    const { user, submit } = setup()

    await user.type(screen.getByLabelText(/Description/), 'Taxi')
    await user.type(screen.getByLabelText(/Amount/), '12.345')

    expect(screen.getByText('Enter a positive amount with up to 2 decimals')).toBeInTheDocument()
    expect(submit()).toBeDisabled()
  })

  it('lets the payer be changed', async () => {
    const { user, requests, submit } = setup()

    await user.type(screen.getByLabelText(/Description/), 'Taxi')
    await user.type(screen.getByLabelText(/Amount/), '9')
    await user.click(screen.getByRole('combobox', { name: /Paid by/ }))
    await user.click(within(screen.getByRole('listbox')).getByRole('option', { name: 'Bob' }))
    await user.click(submit())

    await vi.waitFor(() => expect(requests).toHaveLength(1))
    expect(requests[0]).toMatchObject({ paidBy: 2 })
  })

  it('shows server errors next to the matching fields', async () => {
    server.use(
      http.post('/api/groups/7/expenses', () =>
        HttpResponse.json(
          validationProblem([
            { field: 'paidBy', message: 'member 1 is not in this group' },
            { field: 'splits[1].amount', message: 'must be greater than 0' },
          ]),
          { status: 400 },
        ),
      ),
    )
    const onClose = vi.fn()
    const { user } = renderWithProviders(
      <AddExpenseDialog groupId={7} members={members} onClose={onClose} />,
    )

    await user.type(screen.getByLabelText(/Description/), 'Hotel')
    await user.type(screen.getByLabelText(/Amount/), '10')
    await user.click(screen.getByRole('button', { name: 'Exact amounts' }))
    await user.type(screen.getByRole('textbox', { name: 'Alice' }), '4')
    await user.type(screen.getByRole('textbox', { name: 'Carol' }), '6')
    await user.click(screen.getByRole('button', { name: 'Add expense' }))

    expect(await screen.findByText('member 1 is not in this group')).toBeInTheDocument()
    // splits[1] is the second member sent, i.e. Carol (Bob was left blank).
    const carolField = screen.getByRole('textbox', { name: 'Carol' })
    expect(carolField).toHaveAccessibleDescription('must be greater than 0')
    expect(onClose).not.toHaveBeenCalled()
  })
})
