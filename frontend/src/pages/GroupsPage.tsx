import AddIcon from '@mui/icons-material/Add'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardActionArea from '@mui/material/CardActionArea'
import CardContent from '@mui/material/CardContent'
import Skeleton from '@mui/material/Skeleton'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useGroups } from '../api/hooks'
import { CreateGroupDialog } from '../components/CreateGroupDialog'
import { QueryError } from '../components/QueryError'
import { formatDateTime } from '../lib/dates'

export function GroupsPage() {
  const { data, error, isPending, refetch } = useGroups()
  const [creating, setCreating] = useState(false)
  const navigate = useNavigate()

  return (
    <Stack spacing={3}>
      <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
        <Typography variant="h4" component="h1">
          Groups
        </Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreating(true)}>
          New group
        </Button>
      </Stack>

      {isPending ? (
        <Stack spacing={2}>
          <Skeleton variant="rounded" height={72} />
          <Skeleton variant="rounded" height={72} />
        </Stack>
      ) : error ? (
        <QueryError error={error} onRetry={() => refetch()} />
      ) : data.length === 0 ? (
        <Card variant="outlined">
          <CardContent>
            <Typography>No groups yet.</Typography>
            <Typography color="text.secondary">
              Create a group for a trip, a flat, or anything you share costs for.
            </Typography>
          </CardContent>
        </Card>
      ) : (
        <Stack spacing={2} component="ul" sx={{ listStyle: 'none', p: 0, m: 0 }}>
          {data.map((g) => (
            <Card key={g.id} variant="outlined" component="li">
              <CardActionArea component={Link} to={`/groups/${g.id}`}>
                <CardContent>
                  <Typography variant="h6" component="h2">
                    {g.name}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {g.memberCount} {g.memberCount === 1 ? 'member' : 'members'} · created{' '}
                    {formatDateTime(g.createdAt)}
                  </Typography>
                </CardContent>
              </CardActionArea>
            </Card>
          ))}
        </Stack>
      )}

      {creating && (
        <CreateGroupDialog
          onClose={() => setCreating(false)}
          onCreated={(group) => navigate(`/groups/${group.id}`)}
        />
      )}
    </Stack>
  )
}
