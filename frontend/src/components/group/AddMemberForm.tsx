import Button from '@mui/material/Button'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import { useState, type FormEvent } from 'react'
import { errorMessage, fieldErrors } from '../../api/errors'
import { useAddMember } from '../../api/hooks'

export function AddMemberForm({ groupId }: { groupId: number }) {
  const addMember = useAddMember(groupId)
  const [name, setName] = useState('')

  const submit = (event: FormEvent) => {
    event.preventDefault()
    addMember.mutate(name.trim(), { onSuccess: () => setName('') })
  }

  const error = addMember.isError
    ? (fieldErrors(addMember.error).name ?? errorMessage(addMember.error))
    : undefined

  return (
    <Stack
      component="form"
      onSubmit={submit}
      noValidate
      direction="row"
      spacing={1}
      sx={{ alignItems: 'flex-start' }}
    >
      <TextField
        size="small"
        label="New member"
        value={name}
        onChange={(e) => {
          setName(e.target.value)
          addMember.reset()
        }}
        error={Boolean(error)}
        helperText={error}
        slotProps={{ htmlInput: { maxLength: 100 } }}
      />
      <Button type="submit" variant="outlined" disabled={!name.trim() || addMember.isPending}>
        Add member
      </Button>
    </Stack>
  )
}
