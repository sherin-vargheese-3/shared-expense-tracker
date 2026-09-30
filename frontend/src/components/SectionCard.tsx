import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import type { ReactNode } from 'react'

interface Props {
  title: string
  action?: ReactNode
  children: ReactNode
}

export function SectionCard({ title, action, children }: Props) {
  return (
    <Card variant="outlined" component="section" aria-label={title}>
      <CardContent>
        <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
          <Typography variant="h6" component="h2">
            {title}
          </Typography>
          {action}
        </Stack>
        {children}
      </CardContent>
    </Card>
  )
}
