import { apiRequest } from './client'
import type {
  Balance,
  CreateExpenseRequest,
  CreateGroupRequest,
  CreateSettlementRequest,
  Expense,
  Group,
  GroupSummary,
  Member,
  Settlement,
  SuggestedPayment,
} from './types'

const post = (body: unknown): RequestInit => ({ method: 'POST', body: JSON.stringify(body) })
const del: RequestInit = { method: 'DELETE' }
const group = (groupId: number) => `/api/groups/${groupId}`

export const api = {
  listGroups: () => apiRequest<GroupSummary[]>('/api/groups'),
  getGroup: (groupId: number) => apiRequest<Group>(group(groupId)),
  createGroup: (body: CreateGroupRequest) => apiRequest<Group>('/api/groups', post(body)),
  deleteGroup: (groupId: number) => apiRequest<void>(group(groupId), del),
  addMember: (groupId: number, name: string) =>
    apiRequest<Member>(`${group(groupId)}/members`, post({ name })),
  removeMember: (groupId: number, memberId: number) =>
    apiRequest<void>(`${group(groupId)}/members/${memberId}`, del),

  listExpenses: (groupId: number) => apiRequest<Expense[]>(`${group(groupId)}/expenses`),
  createExpense: (groupId: number, body: CreateExpenseRequest) =>
    apiRequest<Expense>(`${group(groupId)}/expenses`, post(body)),

  getBalances: (groupId: number) => apiRequest<Balance[]>(`${group(groupId)}/balances`),
  getSuggestedPayments: (groupId: number) =>
    apiRequest<SuggestedPayment[]>(`${group(groupId)}/suggested-payments`),

  listSettlements: (groupId: number) => apiRequest<Settlement[]>(`${group(groupId)}/settlements`),
  recordSettlement: (groupId: number, body: CreateSettlementRequest) =>
    apiRequest<Settlement>(`${group(groupId)}/settlements`, post(body)),
}
