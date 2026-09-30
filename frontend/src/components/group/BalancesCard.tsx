import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import Skeleton from '@mui/material/Skeleton'
import Typography from '@mui/material/Typography'
import { useBalances } from '../../api/hooks'
import { apiToCents, formatCents } from '../../lib/money'
import { QueryError } from '../QueryError'
import { SectionCard } from '../SectionCard'

export function BalancesCard({ groupId }: { groupId: number }) {
  const { data, error, isPending, refetch } = useBalances(groupId)

  return (
    <SectionCard title="Balances">
      {isPending ? (
        <Skeleton variant="rounded" height={96} />
      ) : error ? (
        <QueryError error={error} onRetry={() => refetch()} />
      ) : data.length === 0 ? (
        <Typography color="text.secondary">Add members to see balances.</Typography>
      ) : (
        <List dense disablePadding>
          {data.map((b) => {
            const cents = apiToCents(b.balance)
            const [label, color] =
              cents > 0
                ? [`gets back ${formatCents(cents)}`, 'success.main']
                : cents < 0
                  ? [`owes ${formatCents(-cents)}`, 'error.main']
                  : ['settled up', 'text.secondary']
            return (
              <ListItem key={b.memberId} disableGutters>
                <ListItemText primary={b.memberName} />
                <Typography variant="body2" sx={{ color, fontWeight: 500 }}>
                  {label}
                </Typography>
              </ListItem>
            )
          })}
        </List>
      )}
    </SectionCard>
  )
}
