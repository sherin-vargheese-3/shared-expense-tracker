import Alert from '@mui/material/Alert'
import { Route, Routes } from 'react-router'
import { GroupPage } from './pages/GroupPage'
import { GroupsPage } from './pages/GroupsPage'

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<GroupsPage />} />
      <Route path="/groups/:groupId" element={<GroupPage />} />
      <Route path="*" element={<Alert severity="warning">Page not found.</Alert>} />
    </Routes>
  )
}
