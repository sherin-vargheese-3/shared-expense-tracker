import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './endpoints'
import type { CreateExpenseRequest, CreateGroupRequest, CreateSettlementRequest } from './types'

export const queryKeys = {
  groups: ['groups'] as const,
  group: (groupId: number) => ['groups', groupId] as const,
  expenses: (groupId: number) => ['groups', groupId, 'expenses'] as const,
  balances: (groupId: number) => ['groups', groupId, 'balances'] as const,
  suggestedPayments: (groupId: number) => ['groups', groupId, 'suggested-payments'] as const,
  settlements: (groupId: number) => ['groups', groupId, 'settlements'] as const,
}

export const useGroups = () => useQuery({ queryKey: queryKeys.groups, queryFn: api.listGroups })

export const useGroup = (groupId: number) =>
  useQuery({ queryKey: queryKeys.group(groupId), queryFn: () => api.getGroup(groupId) })

export const useExpenses = (groupId: number) =>
  useQuery({ queryKey: queryKeys.expenses(groupId), queryFn: () => api.listExpenses(groupId) })

export const useBalances = (groupId: number) =>
  useQuery({ queryKey: queryKeys.balances(groupId), queryFn: () => api.getBalances(groupId) })

export const useSuggestedPayments = (groupId: number) =>
  useQuery({
    queryKey: queryKeys.suggestedPayments(groupId),
    queryFn: () => api.getSuggestedPayments(groupId),
  })

export const useSettlements = (groupId: number) =>
  useQuery({
    queryKey: queryKeys.settlements(groupId),
    queryFn: () => api.listSettlements(groupId),
  })

export function useCreateGroup() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateGroupRequest) => api.createGroup(body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.groups }),
  })
}

// Any change inside a group can affect its balances and suggestions, so refresh everything under
// the group's key (and the groups list, which shows member counts).

export function useAddMember(groupId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (name: string) => api.addMember(groupId, name),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.groups }),
  })
}

export function useRemoveMember(groupId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (memberId: number) => api.removeMember(groupId, memberId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.groups }),
  })
}

/**
 * Only refreshes the groups list: the deleted group's own queries are dropped by the caller after
 * navigating away, so they don't refetch into a 404 while the page is still mounted.
 */
export function useDeleteGroup(groupId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api.deleteGroup(groupId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.groups, exact: true }),
  })
}

export function useCreateExpense(groupId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateExpenseRequest) => api.createExpense(groupId, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.group(groupId) }),
  })
}

export function useRecordSettlement(groupId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateSettlementRequest) => api.recordSettlement(groupId, body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.group(groupId) }),
  })
}
