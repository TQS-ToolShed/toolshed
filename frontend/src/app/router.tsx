import { createBrowserRouter, Navigate } from "react-router-dom";
import { Suspense, lazy } from "react";
import { LoginPage } from "@/modules/auth/pages/LoginPage";
import { RegisterPage } from "@/modules/auth/pages/RegisterPage";
import { RenterDashboardPage } from "@/modules/renter/pages/RenterDashboardPage";
import { RenterBookingsPage } from "@/modules/renter/pages/RenterBookingsPage";
import { RenterProfilePage } from "@/modules/renter/pages/RenterProfilePage";
import { RenterMyBookingsPage } from "@/modules/renter/pages/RenterMyBookingsPage";
import { PaymentSuccessPage } from "@/modules/renter/pages/PaymentSuccessPage";
import { PaymentCancelledPage } from "@/modules/renter/pages/PaymentCancelledPage";
import { SubscriptionPage } from "@/modules/renter/pages/SubscriptionPage";
import { SubscriptionSuccessPage } from "@/modules/renter/pages/SubscriptionSuccessPage";
import { SupplierDashboardPage } from "@/modules/supplier/pages/SupplierDashboardPage";
import { SupplierToolsPage } from "@/modules/supplier/pages/SupplierToolsPage";
import { SupplierRentalsPage } from "@/modules/supplier/pages/SupplierRentalsPage";
import { AdminDashboardPage } from "@/modules/admin/pages/AdminDashboardPage";
import { ProtectedRoute } from "@/modules/shared/components/ProtectedRoute";

// Lazy load pages
const LoginPage = lazy(() =>
  import("@/modules/auth/pages/LoginPage").then((module) => ({
    default: module.LoginPage,
  }))
);
const RegisterPage = lazy(() =>
  import("@/modules/auth/pages/RegisterPage").then((module) => ({
    default: module.RegisterPage,
  }))
);
const RenterDashboardPage = lazy(() =>
  import("@/modules/renter/pages/RenterDashboardPage").then((module) => ({
    default: module.RenterDashboardPage,
  }))
);
const RenterBookingsPage = lazy(() =>
  import("@/modules/renter/pages/RenterBookingsPage").then((module) => ({
    default: module.RenterBookingsPage,
  }))
);
const RenterProfilePage = lazy(() =>
  import("@/modules/renter/pages/RenterProfilePage").then((module) => ({
    default: module.RenterProfilePage,
  }))
);
const RenterMyBookingsPage = lazy(() =>
  import("@/modules/renter/pages/RenterMyBookingsPage").then((module) => ({
    default: module.RenterMyBookingsPage,
  }))
);
const PaymentSuccessPage = lazy(() =>
  import("@/modules/renter/pages/PaymentSuccessPage").then((module) => ({
    default: module.PaymentSuccessPage,
  }))
);
const PaymentCancelledPage = lazy(() =>
  import("@/modules/renter/pages/PaymentCancelledPage").then((module) => ({
    default: module.PaymentCancelledPage,
  }))
);
const SupplierDashboardPage = lazy(() =>
  import("@/modules/supplier/pages/SupplierDashboardPage").then((module) => ({
    default: module.SupplierDashboardPage,
  }))
);
const SupplierToolsPage = lazy(() =>
  import("@/modules/supplier/pages/SupplierToolsPage").then((module) => ({
    default: module.SupplierToolsPage,
  }))
);
const SupplierRentalsPage = lazy(() =>
  import("@/modules/supplier/pages/SupplierRentalsPage").then((module) => ({
    default: module.SupplierRentalsPage,
  }))
);
const AdminDashboardPage = lazy(() =>
  import("@/modules/admin/pages/AdminDashboardPage").then((module) => ({
    default: module.AdminDashboardPage,
  }))
);

const Loading = () => (
  <div className="flex items-center justify-center min-h-screen">
    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
  </div>
);

export const router = createBrowserRouter([
  {
    path: "/",
    element: <Navigate to="/login" replace />,
  },
  {
    path: "/login",
    element: (
      <Suspense fallback={<Loading />}>
        <LoginPage />
      </Suspense>
    ),
  },
  {
    path: "/register",
    element: (
      <Suspense fallback={<Loading />}>
        <RegisterPage />
      </Suspense>
    ),
  },
  {
    path: "/renter",
    element: (
      <ProtectedRoute allowedRoles={["RENTER"]}>
        <Suspense fallback={<Loading />}>
          <RenterDashboardPage />
        </Suspense>
      </ProtectedRoute>
    ),
  },
  {
    path: "/renter/bookings/:toolId",
    element: (
      <ProtectedRoute allowedRoles={["RENTER"]}>
        <Suspense fallback={<Loading />}>
          <RenterBookingsPage />
        </Suspense>
      </ProtectedRoute>
    ),
  },
  {
    path: "/renter/my-bookings",
    element: (
      <ProtectedRoute allowedRoles={["RENTER"]}>
        <Suspense fallback={<Loading />}>
          <RenterMyBookingsPage />
        </Suspense>
      </ProtectedRoute>
    ),
  },
  {
    path: "/renter/profile",
    element: (
      <ProtectedRoute allowedRoles={["RENTER"]}>
        <Suspense fallback={<Loading />}>
          <RenterProfilePage />
        </Suspense>
      </ProtectedRoute>
    ),
  },
  {
    path: "/supplier",
    element: (
      <ProtectedRoute allowedRoles={["SUPPLIER"]}>
        <Suspense fallback={<Loading />}>
          <SupplierDashboardPage />
        </Suspense>
      </ProtectedRoute>
    ),
  },
  {
    path: "/supplier/tools",
    element: (
      <ProtectedRoute allowedRoles={["SUPPLIER"]}>
        <Suspense fallback={<Loading />}>
          <SupplierToolsPage />
        </Suspense>
      </ProtectedRoute>
    ),
  },
  {
    path: "/supplier/rentals",
    element: (
      <ProtectedRoute allowedRoles={["SUPPLIER"]}>
        <Suspense fallback={<Loading />}>
          <SupplierRentalsPage />
        </Suspense>
      </ProtectedRoute>
    ),
  },
  {
    path: "/admin",
    element: (
      <ProtectedRoute allowedRoles={["ADMIN"]}>
        <Suspense fallback={<Loading />}>
          <AdminDashboardPage />
        </Suspense>
      </ProtectedRoute>
    ),
  },
  // Payment result pages (accessible after Stripe checkout redirect)
  {
    path: "/payment-success",
    element: (
      <Suspense fallback={<Loading />}>
        <PaymentSuccessPage />
      </Suspense>
    ),
  },
  {
    path: "/payment-cancelled",
    element: (
      <Suspense fallback={<Loading />}>
        <PaymentCancelledPage />
      </Suspense>
    ),
  },
  // Subscription pages
  {
    path: "/renter/subscription",
    element: (
      <Suspense fallback={<Loading />}>
        <PaymentSuccessPage />
      </Suspense>
    ),
  },
  {
    path: "/payment-cancelled",
    element: (
      <Suspense fallback={<Loading />}>
        <PaymentCancelledPage />
      </Suspense>
    ),
  },
]);
