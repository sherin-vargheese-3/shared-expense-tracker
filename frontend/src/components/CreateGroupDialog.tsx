import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import { useState, type FormEvent } from 'react'
import { errorMessage, fieldErrors } from '../api/errors'
import { useCreateGroup } from '../api/hooks'
import type { Group } from '../api/types'

interface Props {
  onClose: () => void
  onCreated: (group: Group) => void
}

export function CreateGroupDialog({ onClose, onCreated }: Props) {
  const createGroup = useCreateGroup()
  const [name, setName] = useState('')
  const [memberInput, setMemberInput] = useState('')
  const [members, setMembers] = useState<string[]>([])
  const [memberError, setMemberError] = useState<string>()

  const serverErrors = fieldErrors(createGroup.error)
  const memberServerError = Object.entries(serverErrors).find(([f]) => f.startsWith('members'))?.[1]
  const hasFieldErrors = Object.keys(serverErrors).length > 0

  const addMember = () => {
    const trimmed = memberInput.trim()
    if (!trimmed) {
      return
    }
    if (members.some((m) => m.toLowerCase() === trimmed.toLowerCase())) {
      setMemberError(`${trimmed} is already in the list`)
      return
    }
    setMembers([...members, trimmed])
    setMemberInput('')
    setMemberError(undefined)
  }

  const submit = (event: FormEvent) => {
    event.preventDefault()
    // Include a name that was typed but not yet added with Enter/Add.
    const pending = memberInput.trim()
    const allMembers =
      pending && !members.some((m) => m.toLowerCase() === pending.toLowerCase())
        ? [...members, pending]
        : members
    createGroup.mutate({ name: name.trim(), members: allMembers }, { onSuccess: onCreated })
  }

  return (
    <Dialog open onClose={onClose} fullWidth maxWidth="sm">
      <form onSubmit={submit} noValidate>
        <DialogTitle>New group</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            {createGroup.isError && !hasFieldErrors && (
              <Alert severity="error">{errorMessage(createGroup.error)}</Alert>
            )}
            <TextField
              label="Group name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              autoFocus
              error={Boolean(serverErrors.name)}
              helperText={serverErrors.name}
              slotProps={{ htmlInput: { maxLength: 100 } }}
            />
            <Stack direction="row" spacing={1} sx={{ alignItems: 'flex-start' }}>
              <TextField
                label="Add member"
                value={memberInput}
                onChange={(e) => {
                  setMemberInput(e.target.value)
                  setMemberError(undefined)
                }}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault()
                    addMember()
                  }
                }}
                error={Boolean(memberError ?? memberServerError)}
                helperText={memberError ?? memberServerError ?? 'Press Enter to add'}
                fullWidth
                slotProps={{ htmlInput: { maxLength: 100 } }}
              />
              <Button onClick={addMember} sx={{ mt: 1 }} disabled={!memberInput.trim()}>
                Add
              </Button>
            </Stack>
            {members.length > 0 && (
              <Stack direction="row" useFlexGap sx={{ flexWrap: 'wrap', gap: 1 }} aria-label="Members">
                {members.map((member) => (
                  <Chip
                    key={member}
                    label={member}
                    onDelete={() => setMembers(members.filter((m) => m !== member))}
                  />
                ))}
              </Stack>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Cancel</Button>
          <Button
            type="submit"
            variant="contained"
            disabled={!name.trim() || createGroup.isPending}
          >
            Create group
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}
