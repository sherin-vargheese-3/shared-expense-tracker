import { ApiError } from './client'

/** Maps a 400 response's `errors[]` to `{ field: message }`. */
export function fieldErrors(error: unknown): Record<string, string> {
  if (!(error instanceof ApiError)) {
    return {}
  }
  const result: Record<string, string> = {}
  for (const { field, message } of error.problem.errors ?? []) {
    result[field] ??= message
  }
  return result
}

/** A human-readable message for any error thrown by the API layer. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.problem.detail ?? error.message
  }
  if (error instanceof TypeError) {
    return 'Could not reach the server. Check your connection and try again.'
  }
  return 'Something went wrong. Please try again.'
}

export const isNotFound = (error: unknown): boolean =>
  error instanceof ApiError && error.problem.status === 404
