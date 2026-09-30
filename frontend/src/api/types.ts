export interface Member {
  id: number
  name: string
  /** A former member who still appears in history; a new member may reuse the name. */
  removed: boolean
}

export interface GroupSummary {
  id: number
  name: string
  memberCount: number
  createdAt: string
}

export interface Group {
  id: number
  name: string
  createdAt: string
  members: Member[]
}

export type SplitType = 'EQUAL' | 'EXACT'

export interface Expense {
  id: number
  description: string
  amount: number
  paidBy: Member
  splitType: SplitType
  createdAt: string
  splits: { memberId: number; memberName: string; memberRemoved: boolean; amount: number }[]
}

export interface Balance {
  memberId: number
  memberName: string
  /** Positive: the member is owed money. Negative: the member owes. */
  balance: number
}

export interface SuggestedPayment {
  fromMemberId: number
  fromMemberName: string
  toMemberId: number
  toMemberName: string
  amount: number
}

export interface Settlement {
  id: number
  from: Member
  to: Member
  amount: number
  createdAt: string
}

export interface CreateGroupRequest {
  name: string
  members: string[]
}

export interface CreateExpenseRequest {
  description: string
  amount: number
  paidBy: number
  splitType: SplitType
  splits: { memberId: number; amount?: number }[]
}

export interface CreateSettlementRequest {
  fromMemberId: number
  toMemberId: number
  amount: number
}
