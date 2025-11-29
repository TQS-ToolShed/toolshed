import { createBrowserRouter, Navigate } from "react-router-dom";
import { LoginPage } from "@/modules/auth/pages/LoginPage";
import { RegisterPage } from "@/modules/auth/pages/RegisterPage";
import { RenterDashboardPage } from "@/modules/renter/pages/RenterDashboardPage";
import { SupplierDashboardPage } from "@/modules/supplier/pages/SupplierDashboardPage";
import { AdminDashboardPage } from "@/modules/admin/pages/AdminDashboardPage";
import { ProtectedRoute } from "@/modules/shared/components/ProtectedRoute";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <Navigate to="/login" replace />,
  },
  {
    path: "/login",
    element: <LoginPage />,
  },
  {
    path: "/register",
    element: <RegisterPage />,
  },
  {
    path: "/renter",
    element: (
      <ProtectedRoute allowedRoles={["RENTER"]}>
        <RenterDashboardPage />
      </ProtectedRoute>
    ),
  },
  {
    path: "/supplier",
    element: (
      <ProtectedRoute allowedRoles={["SUPPLIER"]}>
        <SupplierDashboardPage />
      </ProtectedRoute>
    ),
  },
  {
    path: "/admin",
    element: (
      <ProtectedRoute allowedRoles={["ADMIN"]}>
        <AdminDashboardPage />
      </ProtectedRoute>
    ),
  },
]);
