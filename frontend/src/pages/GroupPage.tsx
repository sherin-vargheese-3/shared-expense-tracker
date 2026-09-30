import AddIcon from '@mui/icons-material/Add'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import CancelIcon from '@mui/icons-material/Cancel'
import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import CircularProgress from '@mui/material/CircularProgress'
import Grid from '@mui/material/Grid'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useState } from 'react'
import { Link, useParams } from 'react-router'
import { isNotFound } from '../api/errors'
import { useGroup } from '../api/hooks'
import type { Member } from '../api/types'
import { QueryError } from '../components/QueryError'
import { AddExpenseDialog } from '../components/group/AddExpenseDialog'
import { AddMemberForm } from '../components/group/AddMemberForm'
import { BalancesCard } from '../components/group/BalancesCard'
import { DeleteGroupDialog } from '../components/group/DeleteGroupDialog'
import { ExpensesCard } from '../components/group/ExpensesCard'
import {
  RecordSettlementDialog,
  type SettlementDraft,
} from '../components/group/RecordSettlementDialog'
import { RemoveMemberDialog } from '../components/group/RemoveMemberDialog'
import { SettlementsCard } from '../components/group/SettlementsCard'
import { SuggestedPaymentsCard } from '../components/group/SuggestedPaymentsCard'
import { apiToCents } from '../lib/money'

type OpenDialog =
  | { kind: 'none' }
  | { kind: 'expense' }
  | { kind: 'settlement'; draft?: SettlementDraft }
  | { kind: 'removeMember'; member: Member }
  | { kind: 'deleteGroup' }

const backLink = (
  <Button component={Link} to="/" startIcon={<ArrowBackIcon />} sx={{ alignSelf: 'flex-start' }}>
    All groups
  </Button>
)

export function GroupPage() {
  const groupId = Number(useParams().groupId)
  const validId = Number.isInteger(groupId) && groupId > 0

  if (!validId) {
    return (
      <Stack spacing={2}>
        {backLink}
        <Alert severity="warning">Group not found.</Alert>
      </Stack>
    )
  }
  return <GroupView groupId={groupId} />
}

function GroupView({ groupId }: { groupId: number }) {
  const { data: group, error, isPending, refetch } = useGroup(groupId)
  const [dialog, setDialog] = useState<OpenDialog>({ kind: 'none' })
  const close = () => setDialog({ kind: 'none' })

  if (isPending) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
        <CircularProgress aria-label="Loading group" />
      </Box>
    )
  }
  if (error) {
    return (
      <Stack spacing={2}>
        {backLink}
        {isNotFound(error) ? (
          <Alert severity="warning">Group not found.</Alert>
        ) : (
          <QueryError error={error} onRetry={() => refetch()} />
        )}
      </Stack>
    )
  }

  const { members } = group
  const canAddExpense = members.length > 0
  const canRecordPayment = members.length > 1

  return (
    <Stack spacing={3}>
      {backLink}
      <Stack spacing={1.5}>
        <Stack
          direction="row"
          spacing={2}
          sx={{ alignItems: 'center', justifyContent: 'space-between' }}
        >
          <Typography variant="h4" component="h1">
            {group.name}
          </Typography>
          <Button
            color="error"
            startIcon={<DeleteOutlinedIcon />}
            onClick={() => setDialog({ kind: 'deleteGroup' })}
          >
            Delete group
          </Button>
        </Stack>
        <Stack direction="row" useFlexGap sx={{ flexWrap: 'wrap', gap: 1 }} aria-label="Members">
          {members.length === 0 ? (
            <Typography color="text.secondary">No members yet — add someone to get started.</Typography>
          ) : (
            members.map((m) => (
              <Chip
                key={m.id}
                label={m.name}
                onDelete={() => setDialog({ kind: 'removeMember', member: m })}
                deleteIcon={<CancelIcon titleAccess={`Remove ${m.name}`} />}
              />
            ))
          )}
        </Stack>
        <AddMemberForm groupId={groupId} />
      </Stack>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 5 }}>
          <Stack spacing={3}>
            <BalancesCard groupId={groupId} />
            <SuggestedPaymentsCard
              groupId={groupId}
              onRecord={(p) =>
                setDialog({
                  kind: 'settlement',
                  draft: {
                    fromMemberId: p.fromMemberId,
                    toMemberId: p.toMemberId,
                    amountCents: apiToCents(p.amount),
                  },
                })
              }
            />
          </Stack>
        </Grid>
        <Grid size={{ xs: 12, md: 7 }}>
          <Stack spacing={3}>
            <ExpensesCard
              groupId={groupId}
              action={
                <Button
                  variant="contained"
                  size="small"
                  startIcon={<AddIcon />}
                  disabled={!canAddExpense}
                  onClick={() => setDialog({ kind: 'expense' })}
                >
                  Add expense
                </Button>
              }
            />
            <SettlementsCard
              groupId={groupId}
              action={
                <Button
                  size="small"
                  disabled={!canRecordPayment}
                  onClick={() => setDialog({ kind: 'settlement' })}
                >
                  Record payment
                </Button>
              }
            />
          </Stack>
        </Grid>
      </Grid>

      {dialog.kind === 'expense' && (
        <AddExpenseDialog groupId={groupId} members={members} onClose={close} />
      )}
      {dialog.kind === 'removeMember' && (
        <RemoveMemberDialog groupId={groupId} member={dialog.member} onClose={close} />
      )}
      {dialog.kind === 'deleteGroup' && <DeleteGroupDialog group={group} onClose={close} />}
      {dialog.kind === 'settlement' && (
        <RecordSettlementDialog
          groupId={groupId}
          members={members}
          initial={dialog.draft}
          onClose={close}
        />
      )}
    </Stack>
  )
}
