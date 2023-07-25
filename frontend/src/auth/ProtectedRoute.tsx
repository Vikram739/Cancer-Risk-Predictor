import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import type { RoleName } from '../api/types';
import { useAuth } from './AuthContext';

interface Props {
  children: ReactNode;
  roles?: RoleName[];
}

export default function ProtectedRoute({ children, roles }: Props) {
  const { isAuthenticated, hasRole } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (roles && !hasRole(...roles)) {
    return <Navigate to="/forbidden" replace />;
  }
  return <>{children}</>;
}
