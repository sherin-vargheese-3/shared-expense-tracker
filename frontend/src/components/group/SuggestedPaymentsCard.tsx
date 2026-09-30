import Button from '@mui/material/Button'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import Skeleton from '@mui/material/Skeleton'
import Typography from '@mui/material/Typography'
import { useSuggestedPayments } from '../../api/hooks'
import type { SuggestedPayment } from '../../api/types'
import { formatAmount } from '../../lib/money'
import { QueryError } from '../QueryError'
import { SectionCard } from '../SectionCard'

interface Props {
  groupId: number
  onRecord: (payment: SuggestedPayment) => void
}

export function SuggestedPaymentsCard({ groupId, onRecord }: Props) {
  const { data, error, isPending, refetch } = useSuggestedPayments(groupId)

  return (
    <SectionCard title="Settle up">
      {isPending ? (
        <Skeleton variant="rounded" height={64} />
      ) : error ? (
        <QueryError error={error} onRetry={() => refetch()} />
      ) : data.length === 0 ? (
        <Typography color="text.secondary">Everyone is settled up.</Typography>
      ) : (
        <>
          <Typography variant="body2" color="text.secondary">
            Fewest payments to clear all debts:
          </Typography>
          <List dense disablePadding>
            {data.map((p) => {
              const label = `${p.fromMemberName} pays ${p.toMemberName}`
              return (
                <ListItem
                  key={`${p.fromMemberId}-${p.toMemberId}`}
                  disableGutters
                  secondaryAction={
                    <Button
                      size="small"
                      onClick={() => onRecord(p)}
                      aria-label={`Record: ${label} ${formatAmount(p.amount)}`}
                    >
                      Record
                    </Button>
                  }
                >
                  <ListItemText primary={label} secondary={formatAmount(p.amount)} />
                </ListItem>
              )
            })}
          </List>
        </>
      )}
    </SectionCard>
  )
}
