import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import Skeleton from '@mui/material/Skeleton'
import Typography from '@mui/material/Typography'
import type { ReactNode } from 'react'
import { useExpenses } from '../../api/hooks'
import type { Expense } from '../../api/types'
import { formatDateTime } from '../../lib/dates'
import { formatAmount } from '../../lib/money'
import { QueryError } from '../QueryError'
import { SectionCard } from '../SectionCard'

function splitSummary(expense: Expense): string {
  const shares = expense.splits.map((s) => `${s.memberName} ${formatAmount(s.amount)}`).join(', ')
  const kind = expense.splitType === 'EQUAL' ? 'Split equally' : 'Exact split'
  return `${kind}: ${shares}`
}

export function ExpensesCard({ groupId, action }: { groupId: number; action?: ReactNode }) {
  const { data, error, isPending, refetch } = useExpenses(groupId)

  return (
    <SectionCard title="Expenses" action={action}>
      {isPending ? (
        <Skeleton variant="rounded" height={120} />
      ) : error ? (
        <QueryError error={error} onRetry={() => refetch()} />
      ) : data.length === 0 ? (
        <Typography color="text.secondary">No expenses yet.</Typography>
      ) : (
        <List disablePadding>
          {data.map((e) => (
            <ListItem key={e.id} disableGutters divider alignItems="flex-start">
              <ListItemText
                primary={e.description}
                secondary={
                  <>
                    Paid by {e.paidBy.name} · {formatDateTime(e.createdAt)}
                    <br />
                    {splitSummary(e)}
                  </>
                }
              />
              <Typography sx={{ fontWeight: 500, ml: 2, whiteSpace: 'nowrap' }}>
                {formatAmount(e.amount)}
              </Typography>
            </ListItem>
          ))}
        </List>
      )}
    </SectionCard>
  )
}
