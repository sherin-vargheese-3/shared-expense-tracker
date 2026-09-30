import CssBaseline from '@mui/material/CssBaseline'
import { ThemeProvider } from '@mui/material/styles'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import { ApiError } from './api/client'
import App from './App'
import { theme } from './theme'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Retrying 4xx responses (e.g. 404) is pointless; retry network/5xx errors once.
      retry: (failureCount, error) =>
        failureCount < 1 && !(error instanceof ApiError && error.problem.status < 500),
      refetchOnWindowFocus: false,
    },
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </ThemeProvider>
    </QueryClientProvider>
  </StrictMode>,
)
