import AppBar from '@mui/material/AppBar'
import Box from '@mui/material/Box'
import Container from '@mui/material/Container'
import Toolbar from '@mui/material/Toolbar'
import Typography from '@mui/material/Typography'
import { Link } from 'react-router'
import { AppRoutes } from './AppRoutes'
import { ApiStatus } from './components/ApiStatus'

export default function App() {
  return (
    <>
      <AppBar position="static" elevation={0}>
        <Toolbar>
          <Typography
            variant="h6"
            component={Link}
            to="/"
            sx={{ flexGrow: 1, color: 'inherit', textDecoration: 'none' }}
          >
            Shared Expense Tracker
          </Typography>
          <Box sx={{ bgcolor: 'background.paper', borderRadius: 4 }}>
            <ApiStatus />
          </Box>
        </Toolbar>
      </AppBar>
      <Container maxWidth="lg" sx={{ py: 4 }}>
        <AppRoutes />
      </Container>
    </>
  )
}
