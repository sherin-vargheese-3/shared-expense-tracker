import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import Skeleton from '@mui/material/Skeleton'
import Typography from '@mui/material/Typography'
import type { ReactNode } from 'react'
import { useSettlements } from '../../api/hooks'
import { formatDateTime } from '../../lib/dates'
import { displayName } from '../../lib/members'
import { formatAmount } from '../../lib/money'
import { QueryError } from '../QueryError'
import { SectionCard } from '../SectionCard'

export function SettlementsCard({ groupId, action }: { groupId: number; action?: ReactNode }) {
  const { data, error, isPending, refetch } = useSettlements(groupId)

  return (
    <SectionCard title="Payments" action={action}>
      {isPending ? (
        <Skeleton variant="rounded" height={64} />
      ) : error ? (
        <QueryError error={error} onRetry={() => refetch()} />
      ) : data.length === 0 ? (
        <Typography color="text.secondary">No payments recorded yet.</Typography>
      ) : (
        <List dense disablePadding>
          {data.map((s) => (
            <ListItem key={s.id} disableGutters>
              <ListItemText
                primary={`${displayName(s.from.name, s.from.removed)} paid ${displayName(
                  s.to.name,
                  s.to.removed,
                )}`}
                secondary={formatDateTime(s.createdAt)}
              />
              <Typography sx={{ fontWeight: 500, ml: 2 }}>{formatAmount(s.amount)}</Typography>
            </ListItem>
          ))}
        </List>
      )}
    </SectionCard>
  )
}
