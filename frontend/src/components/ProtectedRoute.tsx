import { Navigate } from 'react-router-dom'
import { getUser, isLoggedIn, Role } from '../store/auth'

interface Props {
  children: React.ReactNode
  roles?: Role[]
}

export default function ProtectedRoute({ children, roles }: Props) {
  if (!isLoggedIn()) return <Navigate to="/login" replace />
  const user = getUser()
  if (roles && user && !roles.includes(user.role)) return <Navigate to="/" replace />
  return <>{children}</>
}
