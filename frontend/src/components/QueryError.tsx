import Alert from '@mui/material/Alert'
import Button from '@mui/material/Button'
import { errorMessage } from '../api/errors'

interface Props {
  error: unknown
  onRetry?: () => void
}

export function QueryError({ error, onRetry }: Props) {
  return (
    <Alert
      severity="error"
      action={
        onRetry && (
          <Button color="inherit" size="small" onClick={onRetry}>
            Retry
          </Button>
        )
      }
    >
      {errorMessage(error)}
    </Alert>
  )
}
