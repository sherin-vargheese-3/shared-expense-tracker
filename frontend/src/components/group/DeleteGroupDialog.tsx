import Alert from '@mui/material/Alert'
import Typography from '@mui/material/Typography'
import { useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { queryKeys, useBalances, useDeleteGroup } from '../../api/hooks'
import type { Group } from '../../api/types'
import { apiToCents } from '../../lib/money'
import { ConfirmDialog } from '../ConfirmDialog'

interface Props {
  group: Group
  onClose: () => void
}

export function DeleteGroupDialog({ group, onClose }: Props) {
  const deleteGroup = useDeleteGroup(group.id)
  const { data: balances } = useBalances(group.id)
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const unsettled = balances?.some((b) => apiToCents(b.balance) !== 0) ?? false

  const confirm = () =>
    deleteGroup.mutate(undefined, {
      onSuccess: () => {
        navigate('/', { replace: true })
        queryClient.removeQueries({ queryKey: queryKeys.group(group.id) })
      },
    })

  return (
    <ConfirmDialog
      title={`Delete ${group.name}?`}
      confirmLabel="Delete group"
      onConfirm={confirm}
      onClose={onClose}
      pending={deleteGroup.isPending}
      error={deleteGroup.error}
    >
      {unsettled && (
        <Alert severity="warning">Some balances are not settled yet.</Alert>
      )}
      <Typography>
        This permanently deletes the group with all its members, expenses and payments. This can't
        be undone.
      </Typography>
    </ConfirmDialog>
  )
}
