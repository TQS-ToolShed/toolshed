import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';

interface ProtectedRouteProps {
  children: ReactNode;
  allowedRoles: string[];
}

export const ProtectedRoute = ({ children, allowedRoles }: ProtectedRouteProps) => {
  // TODO: Implement real auth logic
  const userRole = "RENTER"; // Mock role for now
  const isAuthenticated = true; // Mock auth status

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  // Simple check, in real app might need more robust logic
  if (!allowedRoles.includes(userRole) && !allowedRoles.includes("BOTH")) {
    // Redirect to home or unauthorized page
    return <Navigate to="/" replace />; 
  }

  return <>{children}</>;
};
