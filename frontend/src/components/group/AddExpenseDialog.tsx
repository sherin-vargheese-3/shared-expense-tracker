import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Checkbox from '@mui/material/Checkbox'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import FormControlLabel from '@mui/material/FormControlLabel'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import ToggleButton from '@mui/material/ToggleButton'
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup'
import Typography from '@mui/material/Typography'
import { useState, type FormEvent } from 'react'
import { errorMessage, fieldErrors } from '../../api/errors'
import { useCreateExpense } from '../../api/hooks'
import type { CreateExpenseRequest, Member, SplitType } from '../../api/types'
import { centsToApi, equalSplit, formatCents, parseAmount } from '../../lib/money'

interface Props {
  groupId: number
  members: Member[]
  onClose: () => void
}

const AMOUNT_HINT = 'Enter a positive amount with up to 2 decimals'

export function AddExpenseDialog({ groupId, members, onClose }: Props) {
  const createExpense = useCreateExpense(groupId)
  const [description, setDescription] = useState('')
  const [amount, setAmount] = useState('')
  const [paidBy, setPaidBy] = useState<number | ''>(members[0]?.id ?? '')
  const [splitType, setSplitType] = useState<SplitType>('EQUAL')
  const [selected, setSelected] = useState<Set<number>>(() => new Set(members.map((m) => m.id)))
  const [exactAmounts, setExactAmounts] = useState<Record<number, string>>({})
  // Member ids in the order they were sent, to map `splits[i]` server errors back to a member.
  const [sentOrder, setSentOrder] = useState<number[]>([])

  const totalCents = parseAmount(amount)
  const amountInvalid = amount.trim() !== '' && (totalCents === null || totalCents === 0)

  // EQUAL: checked members share the amount.
  const equalShares = equalSplit(totalCents ?? 0, [...selected])

  // EXACT: every member with a non-blank amount participates.
  const exactEntries = members
    .filter((m) => (exactAmounts[m.id] ?? '').trim() !== '')
    .map((m) => ({ memberId: m.id, cents: parseAmount(exactAmounts[m.id]) }))
  const exactInvalid = exactEntries.some((e) => e.cents === null || e.cents === 0)
  const allocatedCents = exactEntries.reduce((sum, e) => sum + (e.cents ?? 0), 0)
  const remainingCents = (totalCents ?? 0) - allocatedCents

  const splitValid =
    splitType === 'EQUAL'
      ? selected.size > 0
      : exactEntries.length > 0 && !exactInvalid && remainingCents === 0
  const canSubmit =
    description.trim() !== '' &&
    totalCents !== null &&
    totalCents > 0 &&
    paidBy !== '' &&
    splitValid &&
    !createExpense.isPending

  const serverErrors = fieldErrors(createExpense.error)
  const splitServerError = (memberId: number, field: 'amount' | 'memberId') => {
    const index = sentOrder.indexOf(memberId)
    return index >= 0 ? serverErrors[`splits[${index}].${field}`] : undefined
  }
  const knownFields = ['description', 'amount', 'paidBy', 'splitType']
  const generalError =
    createExpense.isError &&
    (serverErrors.splits ??
      (Object.keys(serverErrors).some((f) => knownFields.includes(f) || f.startsWith('splits['))
        ? undefined
        : errorMessage(createExpense.error)))

  const toggleMember = (memberId: number) => {
    const next = new Set(selected)
    if (next.has(memberId)) {
      next.delete(memberId)
    } else {
      next.add(memberId)
    }
    setSelected(next)
  }

  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (!canSubmit) {
      return
    }
    const splits: CreateExpenseRequest['splits'] =
      splitType === 'EQUAL'
        ? members.filter((m) => selected.has(m.id)).map((m) => ({ memberId: m.id }))
        : exactEntries.map((e) => ({ memberId: e.memberId, amount: centsToApi(e.cents ?? 0) }))
    setSentOrder(splits.map((s) => s.memberId))
    createExpense.mutate(
      {
        description: description.trim(),
        amount: centsToApi(totalCents),
        paidBy,
        splitType,
        splits,
      },
      { onSuccess: onClose },
    )
  }

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm">
      <form onSubmit={submit} noValidate>
        <DialogTitle>Add expense</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            {generalError && <Alert severity="error">{generalError}</Alert>}
            <TextField
              label="Description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              required
              autoFocus
              error={Boolean(serverErrors.description)}
              helperText={serverErrors.description}
              slotProps={{ htmlInput: { maxLength: 255 } }}
            />
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                label="Amount"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                required
                error={amountInvalid || Boolean(serverErrors.amount)}
                helperText={amountInvalid ? AMOUNT_HINT : serverErrors.amount}
                slotProps={{ htmlInput: { inputMode: 'decimal' } }}
                sx={{ flex: 1 }}
              />
              <TextField
                select
                label="Paid by"
                value={paidBy}
                onChange={(e) => setPaidBy(Number(e.target.value))}
                required
                error={Boolean(serverErrors.paidBy)}
                helperText={serverErrors.paidBy}
                sx={{ flex: 1 }}
              >
                {members.map((m) => (
                  <MenuItem key={m.id} value={m.id}>
                    {m.name}
                  </MenuItem>
                ))}
              </TextField>
            </Stack>

            <ToggleButtonGroup
              exclusive
              fullWidth
              size="small"
              value={splitType}
              onChange={(_, value: SplitType | null) => value && setSplitType(value)}
              aria-label="Split type"
            >
              <ToggleButton value="EQUAL">Split equally</ToggleButton>
              <ToggleButton value="EXACT">Exact amounts</ToggleButton>
            </ToggleButtonGroup>

            {splitType === 'EQUAL' ? (
              <Stack>
                {members.map((m) => (
                  <Stack
                    key={m.id}
                    direction="row"
                    sx={{ alignItems: 'center', justifyContent: 'space-between' }}
                  >
                    <FormControlLabel
                      control={
                        <Checkbox checked={selected.has(m.id)} onChange={() => toggleMember(m.id)} />
                      }
                      label={m.name}
                    />
                    <Typography variant="body2" color="text.secondary">
                      {selected.has(m.id) && totalCents
                        ? formatCents(equalShares.get(m.id) ?? 0)
                        : '—'}
                    </Typography>
                  </Stack>
                ))}
                {selected.size === 0 && (
                  <Typography variant="body2" color="error">
                    Select at least one member
                  </Typography>
                )}
              </Stack>
            ) : (
              <Stack spacing={1.5}>
                <Typography variant="body2" color="text.secondary">
                  Leave a member blank to exclude them.
                </Typography>
                {members.map((m) => {
                  const value = exactAmounts[m.id] ?? ''
                  const cents = value.trim() === '' ? null : parseAmount(value)
                  const invalid = value.trim() !== '' && (cents === null || cents === 0)
                  const serverError = splitServerError(m.id, 'amount')
                  return (
                    <TextField
                      key={m.id}
                      size="small"
                      label={m.name}
                      value={value}
                      onChange={(e) => setExactAmounts({ ...exactAmounts, [m.id]: e.target.value })}
                      error={invalid || Boolean(serverError)}
                      helperText={invalid ? AMOUNT_HINT : serverError}
                      slotProps={{ htmlInput: { inputMode: 'decimal' } }}
                    />
                  )
                })}
                <Typography
                  role="status"
                  aria-live="polite"
                  variant="body2"
                  color={remainingCents === 0 && totalCents ? 'success.main' : 'warning.main'}
                >
                  {totalCents === null || totalCents === 0
                    ? 'Enter the total amount first'
                    : remainingCents === 0
                      ? 'Fully allocated'
                      : remainingCents > 0
                        ? `Remaining: ${formatCents(remainingCents)}`
                        : `Over by ${formatCents(-remainingCents)}`}
                </Typography>
              </Stack>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={!canSubmit}>
            Add expense
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
