import Alert from '@mui/material/Alert'
import Typography from '@mui/material/Typography'
import { useBalances, useRemoveMember } from '../../api/hooks'
import type { Member } from '../../api/types'
import { apiToCents, formatCents } from '../../lib/money'
import { ConfirmDialog } from '../ConfirmDialog'

interface Props {
  groupId: number
  member: Member
  onClose: () => void
}

export function RemoveMemberDialog({ groupId, member, onClose }: Props) {
  const removeMember = useRemoveMember(groupId)
  const { data: balances } = useBalances(groupId)

  // The server enforces this too; checking here explains it before the user tries.
  const balance = balances?.find((b) => b.memberId === member.id)
  const cents = balance ? apiToCents(balance.balance) : 0
  const unsettled =
    cents < 0
      ? `${member.name} owes ${formatCents(-cents)}.`
      : cents > 0
        ? `${member.name} is owed ${formatCents(cents)}.`
        : undefined

  return (
    <ConfirmDialog
      title={`Remove ${member.name}?`}
      confirmLabel="Remove"
      onConfirm={() => removeMember.mutate(member.id, { onSuccess: onClose })}
      onClose={onClose}
      pending={removeMember.isPending}
      error={removeMember.error}
      confirmDisabled={Boolean(unsettled)}
    >
      {unsettled ? (
        <Alert severity="warning">{unsettled} Settle up before removing them.</Alert>
      ) : (
        <Typography>
          {member.name} will no longer be part of this group. Past expenses and payments will still
          show their name.
        </Typography>
      )}
    </ConfirmDialog>
  )
}
