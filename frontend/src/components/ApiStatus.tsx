import Chip from '@mui/material/Chip'
import { useEffect, useState } from 'react'
import { fetchHealth } from '../api/health'

type Status = 'checking' | 'up' | 'down'

export function ApiStatus() {
  const [status, setStatus] = useState<Status>('checking')

  useEffect(() => {
    let active = true
    fetchHealth()
      .then((health) => active && setStatus(health.status === 'UP' ? 'up' : 'down'))
      .catch(() => active && setStatus('down'))
    return () => {
      active = false
    }
  }, [])

  const label = { checking: 'API: checking…', up: 'API: online', down: 'API: offline' }[status]
  const color = { checking: 'default', up: 'success', down: 'error' } as const

  return <Chip label={label} color={color[status]} size="small" variant="outlined" />
}
