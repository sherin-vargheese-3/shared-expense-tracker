/** RFC 7807 problem response returned by the backend on errors. */
export interface ProblemDetail {
  type?: string
  title?: string
  status: number
  detail?: string
  errors?: { field: string; message: string }[]
}

export class ApiError extends Error {
  readonly problem: ProblemDetail

  constructor(problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? `Request failed with status ${problem.status}`)
    this.name = 'ApiError'
    this.problem = problem
  }
}

export async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  })

  if (!response.ok) {
    let problem: ProblemDetail = { status: response.status }
    try {
      problem = { ...problem, ...(await response.json()) }
    } catch {
      // Non-JSON error body; keep the status-only problem.
    }
    throw new ApiError(problem)
  }

  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}
