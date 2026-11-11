import { Navigate } from 'react-router-dom';
import { useAuth, homeFor } from '../auth/AuthContext';

export default function ProtectedRoute({ roles, allowPending = false, children }) {
  const { user } = useAuth();
    if (!user) return <Navigate to="/welcome" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to={homeFor(user)} replace />;
  if (roles && user.role === 'ACCOUNT_HOLDER' && user.status !== 'ACTIVE' && !allowPending) {
    return <Navigate to="/onboarding" replace />;
  }
  return children;
}