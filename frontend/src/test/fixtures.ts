import type { Group, Member } from '../api/types'

export const alice: Member = { id: 1, name: 'Alice' }
export const bob: Member = { id: 2, name: 'Bob' }
export const carol: Member = { id: 3, name: 'Carol' }

export const tripGroup: Group = {
  id: 7,
  name: 'Goa trip',
  createdAt: '2026-09-01T10:00:00Z',
  members: [alice, bob, carol],
}

/** A 400 ProblemDetail body in the backend's shape. */
export const validationProblem = (errors: { field: string; message: string }[]) => ({
  type: 'about:blank',
  title: 'Validation failed',
  status: 400,
  detail: 'One or more fields are invalid',
  errors,
})
