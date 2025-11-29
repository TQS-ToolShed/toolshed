import { createBrowserRouter } from "react-router-dom";
import { LoginPage } from "@/modules/auth/pages/LoginPage";
import { RegisterPage } from "@/modules/auth/pages/RegisterPage";
import { RenterDashboardPage } from "@/modules/renter/pages/RenterDashboardPage";
import { OwnerDashboardPage } from "@/modules/owner/pages/OwnerDashboardPage";
import { AdminDashboardPage } from "@/modules/admin/pages/AdminDashboardPage";
import { ProtectedRoute } from "@/modules/shared/components/ProtectedRoute";

export const router = createBrowserRouter([
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
      <ProtectedRoute allowedRoles={["RENTER", "BOTH"]}>
        <RenterDashboardPage />
      </ProtectedRoute>
    ),
  },
  {
    path: "/owner",
    element: (
      <ProtectedRoute allowedRoles={["OWNER", "BOTH"]}>
        <OwnerDashboardPage />
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
