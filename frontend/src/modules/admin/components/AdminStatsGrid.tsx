import type { AdminStats } from "../api/admin-api";

interface StatCardConfig {
  label: string;
  value: number;
  color: string;
}

interface AdminStatsGridProps {
  stats: AdminStats;
}

export const AdminStatsGrid = ({ stats }: AdminStatsGridProps) => {
  const statCards: StatCardConfig[] = [
    { label: "Total Users", value: stats.totalUsers, color: "text-gray-800" },
    {
      label: "Active Users",
      value: stats.activeUsers,
      color: "text-green-600",
    },
    {
      label: "Inactive Users",
      value: stats.inactiveUsers,
      color: "text-red-600",
    },
    {
      label: "Total Bookings",
      value: stats.totalBookings,
      color: "text-gray-800",
    },
    {
      label: "Active Bookings",
      value: stats.activeBookings,
      color: "text-blue-600",
    },
    {
      label: "Completed Bookings",
      value: stats.completedBookings,
      color: "text-green-600",
    },
    {
      label: "Cancelled Bookings",
      value: stats.cancelledBookings,
      color: "text-red-600",
    },
  ];

  return (
    <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
      {statCards.map((card) => (
        <div
          key={card.label}
          className=" p-6 rounded-lg border border-gray-300"
        >
          <h3 className="text-gray-500 text-sm font-medium uppercase">
            {card.label}
          </h3>
          <p className={`text-3xl font-bold mt-2 ${card.color}`}>
            {card.value}
          </p>
        </div>
      ))}
    </div>
  );
};
