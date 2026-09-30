import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Stack from '@mui/material/Stack'
import type { ReactNode } from 'react'
import { errorMessage } from '../api/errors'

interface Props {
  title: string
  children: ReactNode
  confirmLabel: string
  onConfirm: () => void
  onClose: () => void
  pending?: boolean
  error?: unknown
  confirmDisabled?: boolean
}

/** A confirmation dialog for destructive actions; shows any API error from the action. */
export function ConfirmDialog({
  title,
  children,
  confirmLabel,
  onConfirm,
  onClose,
  pending = false,
  error,
  confirmDisabled = false,
}: Props) {
  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="xs">
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <Stack spacing={2}>
          {children}
          {error != null && <Alert severity="error">{errorMessage(error)}</Alert>}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button
          variant="contained"
          color="error"
          onClick={onConfirm}
          disabled={pending || confirmDisabled}
        >
          {confirmLabel}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
