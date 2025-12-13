import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "@/modules/auth/context/AuthContext";

interface ProtectedRouteProps {
  children: ReactNode;
  allowedRoles: string[];
}

export const ProtectedRoute = ({
  children,
  allowedRoles,
}: ProtectedRouteProps) => {
  const { isAuthenticated, role, isLoading } = useAuth();

  if (isLoading) {
    // You could replace this with a proper Spinner/Loading component
    return (
      <div className="flex justify-center items-center h-screen">
        Loading...
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  // If there's no role, or the role is not allowed, redirect
  if (!role || !allowedRoles.includes(role)) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
};
