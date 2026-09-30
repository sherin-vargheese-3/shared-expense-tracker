import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import { useState, type FormEvent } from 'react'
import { errorMessage, fieldErrors } from '../../api/errors'
import { useRecordSettlement } from '../../api/hooks'
import type { Member } from '../../api/types'
import { centsToApi, centsToInput, parseAmount } from '../../lib/money'

export interface SettlementDraft {
  fromMemberId: number
  toMemberId: number
  amountCents: number
}

interface Props {
  groupId: number
  members: Member[]
  initial?: SettlementDraft
  onClose: () => void
}

export function RecordSettlementDialog({ groupId, members, initial, onClose }: Props) {
  const recordSettlement = useRecordSettlement(groupId)
  const [from, setFrom] = useState<number | ''>(initial?.fromMemberId ?? '')
  const [to, setTo] = useState<number | ''>(initial?.toMemberId ?? '')
  const [amount, setAmount] = useState(initial ? centsToInput(initial.amountCents) : '')

  const cents = parseAmount(amount)
  const amountInvalid = amount.trim() !== '' && (cents === null || cents === 0)
  const sameMember = from !== '' && from === to
  const canSubmit =
    from !== '' && to !== '' && !sameMember && cents !== null && cents > 0 && !recordSettlement.isPending

  const serverErrors = fieldErrors(recordSettlement.error)
  const hasFieldErrors = Object.keys(serverErrors).length > 0

  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (!canSubmit) {
      return
    }
    recordSettlement.mutate(
      { fromMemberId: from, toMemberId: to, amount: centsToApi(cents) },
      { onSuccess: onClose },
    )
  }

  const memberSelect = (
    label: string,
    value: number | '',
    onChange: (id: number) => void,
    error?: string,
  ) => (
    <TextField
      select
      label={label}
      value={value}
      onChange={(e) => onChange(Number(e.target.value))}
      required
      error={Boolean(error)}
      helperText={error}
      sx={{ flex: 1 }}
    >
      {members.map((m) => (
        <MenuItem key={m.id} value={m.id}>
          {m.name}
        </MenuItem>
      ))}
    </TextField>
  )

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="xs">
      <form onSubmit={submit} noValidate>
        <DialogTitle>Record payment</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            {recordSettlement.isError && !hasFieldErrors && (
              <Alert severity="error">{errorMessage(recordSettlement.error)}</Alert>
            )}
            {memberSelect('From', from, setFrom, serverErrors.fromMemberId)}
            {memberSelect(
              'To',
              to,
              setTo,
              sameMember ? 'Choose a different member' : serverErrors.toMemberId,
            )}
            <TextField
              label="Amount"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              required
              error={amountInvalid || Boolean(serverErrors.amount)}
              helperText={
                amountInvalid ? 'Enter a positive amount with up to 2 decimals' : serverErrors.amount
              }
              slotProps={{ htmlInput: { inputMode: 'decimal' } }}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={!canSubmit}>
            Record payment
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
