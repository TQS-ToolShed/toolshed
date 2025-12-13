import { createBrowserRouter, Navigate } from "react-router-dom";
import { LoginPage } from "@/modules/auth/pages/LoginPage";
import { RegisterPage } from "@/modules/auth/pages/RegisterPage";
import { RenterDashboardPage } from "@/modules/renter/pages/RenterDashboardPage";
import { RenterBookingsPage } from "@/modules/renter/pages/RenterBookingsPage";
import { RenterProfilePage } from "@/modules/renter/pages/RenterProfilePage";
import { RenterMyBookingsPage } from "@/modules/renter/pages/RenterMyBookingsPage";
import { PaymentSuccessPage } from "@/modules/renter/pages/PaymentSuccessPage";
import { PaymentCancelledPage } from "@/modules/renter/pages/PaymentCancelledPage";
import { SupplierDashboardPage } from "@/modules/supplier/pages/SupplierDashboardPage";
import { SupplierToolsPage } from "@/modules/supplier/pages/SupplierToolsPage";
import { SupplierRentalsPage } from "@/modules/supplier/pages/SupplierRentalsPage";
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
    path: "/renter/bookings/:toolId",
    element: (
      <ProtectedRoute allowedRoles={["RENTER"]}>
        <RenterBookingsPage />
      </ProtectedRoute>
    ),
  },
  {
    path: "/renter/my-bookings",
    element: (
      <ProtectedRoute allowedRoles={["RENTER"]}>
        <RenterMyBookingsPage />
      </ProtectedRoute>
    ),
  },
  {
    path: "/renter/profile",
    element: (
      <ProtectedRoute allowedRoles={["RENTER"]}>
        <RenterProfilePage />
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
    path: "/supplier/tools",
    element: (
      <ProtectedRoute allowedRoles={["SUPPLIER"]}>
        <SupplierToolsPage />
      </ProtectedRoute>
    ),
  },
  {
    path: "/supplier/rentals",
    element: (
      <ProtectedRoute allowedRoles={["SUPPLIER"]}>
        <SupplierRentalsPage />
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
  // Payment result pages (accessible after Stripe checkout redirect)
  {
    path: "/payment-success",
    element: <PaymentSuccessPage />,
  },
  {
    path: "/payment-cancelled",
    element: <PaymentCancelledPage />,
  },
]);
