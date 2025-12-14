import { useEffect, useState } from "react";
import { useAuth } from "@/modules/auth/context/AuthContext";
import type { User } from "@/modules/auth/context/AuthContext";
import {
  getAdminStats,
  getUsers,
  activateUser,
  deactivateUser,
} from "../api/admin-api";
import type { AdminStats } from "../api/admin-api";

export const AdminDashboardPage = () => {
  const { user } = useAuth();
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [users, setUsers] = useState<User[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    try {
      setIsLoading(true);
      const [statsData, usersData] = await Promise.all([
        getAdminStats(),
        getUsers(),
      ]);
      setStats(statsData);
      setUsers(usersData);
    } catch (err) {
      setError("Failed to load dashboard data");
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleToggleStatus = async (targetUser: User) => {
    try {
      if (targetUser.status === "ACTIVE") {
        await deactivateUser(targetUser.id);
      } else {
        await activateUser(targetUser.id);
      }
      // Refresh user list
      const updatedUsers = await getUsers();
      setUsers(updatedUsers);
    } catch (err) {
      console.error("Failed to update user status", err);
      alert("Failed to update user status");
    }
  };

  if (isLoading) return <div className="p-8">Loading...</div>;
  if (error) return <div className="p-8 text-red-500">{error}</div>;

  return (
    <div className="container mx-auto p-6">
      <h1 className="text-3xl font-bold mb-8">Admin Dashboard</h1>

      {/* Stats Section */}
      {stats && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
          <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
            <h3 className="text-gray-500 text-sm font-medium uppercase">
              Total Users
            </h3>
            <p className="text-3xl font-bold text-gray-800 mt-2">
              {stats.totalUsers}
            </p>
          </div>
          <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
            <h3 className="text-gray-500 text-sm font-medium uppercase">
              Active Users
            </h3>
            <p className="text-3xl font-bold text-green-600 mt-2">
              {stats.activeUsers}
            </p>
          </div>
          <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
            <h3 className="text-gray-500 text-sm font-medium uppercase">
              Inactive Users
            </h3>
            <p className="text-3xl font-bold text-red-600 mt-2">
              {stats.inactiveUsers}
            </p>
          </div>
          <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
            <h3 className="text-gray-500 text-sm font-medium uppercase">
              Total Bookings
            </h3>
            <p className="text-3xl font-bold text-gray-800 mt-2">
              {stats.totalBookings}
            </p>
          </div>
          <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
            <h3 className="text-gray-500 text-sm font-medium uppercase">
              Active Bookings
            </h3>
            <p className="text-3xl font-bold text-blue-600 mt-2">
              {stats.activeBookings}
            </p>
          </div>
          <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
            <h3 className="text-gray-500 text-sm font-medium uppercase">
              Completed Bookings
            </h3>
            <p className="text-3xl font-bold text-green-600 mt-2">
              {stats.completedBookings}
            </p>
          </div>
          <div className="bg-white p-6 rounded-lg shadow-md border border-gray-200">
            <h3 className="text-gray-500 text-sm font-medium uppercase">
              Cancelled Bookings
            </h3>
            <p className="text-3xl font-bold text-red-600 mt-2">
              {stats.cancelledBookings}
            </p>
          </div>
        </div>
      )}

      {/* Users Section */}
      <div className="bg-white rounded-lg shadow-md border border-gray-200 overflow-hidden">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-xl font-bold text-gray-800">User Management</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-gray-600">
            <thead className="bg-gray-50 text-gray-700 uppercase font-medium">
              <tr>
                <th className="px-6 py-3">Name</th>
                <th className="px-6 py-3">Email</th>
                <th className="px-6 py-3">Role</th>
                <th className="px-6 py-3">Status</th>
                <th className="px-6 py-3">Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr
                  key={u.id}
                  className="border-t border-gray-100 hover:bg-gray-50"
                >
                  <td className="px-6 py-4 font-medium text-gray-900">
                    {u.firstName} {u.lastName}
                  </td>
                  <td className="px-6 py-4">{u.email}</td>
                  <td className="px-6 py-4">
                    <span
                      className={`inline-block px-2 py-1 text-xs rounded-full ${
                        u.role === "ADMIN"
                          ? "bg-purple-100 text-purple-800"
                          : u.role === "SUPPLIER"
                          ? "bg-blue-100 text-blue-800"
                          : "bg-gray-100 text-gray-800"
                      }`}
                    >
                      {u.role}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    <span
                      className={`inline-block px-2 py-1 text-xs rounded-full ${
                        u.status === "ACTIVE"
                          ? "bg-green-100 text-green-800"
                          : u.status === "SUSPENDED"
                          ? "bg-red-100 text-red-800"
                          : "bg-yellow-100 text-yellow-800"
                      }`}
                    >
                      {u.status || "ACTIVE"}
                    </span>
                  </td>
                  <td className="px-6 py-4">
                    {u.id !== user?.id && (
                      <button
                        onClick={() => handleToggleStatus(u)}
                        className={`text-xs font-medium px-3 py-1.5 rounded transition ${
                          u.status === "ACTIVE"
                            ? "bg-red-50 text-red-600 hover:bg-red-100"
                            : "bg-green-50 text-green-600 hover:bg-green-100"
                        }`}
                      >
                        {u.status === "ACTIVE" ? "Deactivate" : "Activate"}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
