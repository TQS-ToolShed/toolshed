import { useEffect, useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { getOwnerEarnings, type MonthlyEarnings } from '../api/earnings-api';
import { useUser } from '@/contexts/UserContext';
import { Loader2 } from 'lucide-react';

const MONTH_NAMES = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December'
];

export const OwnerEarnings = () => {
  const { user } = useUser();
  const [monthlyEarnings, setMonthlyEarnings] = useState<MonthlyEarnings[]>([]);
  const [totalEarnings, setTotalEarnings] = useState<number>(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchEarnings = async () => {
      if (!user?.id) return;

      try {
        setLoading(true);
        setError(null);
        const data = await getOwnerEarnings(user.id);
        setMonthlyEarnings(data.monthlyEarnings);
        setTotalEarnings(data.totalEarnings);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load earnings');
      } finally {
        setLoading(false);
      }
    };

    fetchEarnings();
  }, [user?.id]);

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Earnings Overview</CardTitle>
          <CardDescription>Track your rental income by month</CardDescription>
        </CardHeader>
        <CardContent className="flex justify-center items-center py-8">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Earnings Overview</CardTitle>
          <CardDescription>Track your rental income by month</CardDescription>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-destructive">{error}</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Earnings Overview</CardTitle>
        <CardDescription>Track your rental income by month</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {/* Total Earnings */}
          <div className="bg-primary/10 p-4 rounded-lg">
            <p className="text-sm font-medium text-muted-foreground">Total Earnings</p>
            <p className="text-3xl font-bold text-primary">€{totalEarnings.toFixed(2)}</p>
          </div>

          {/* Monthly Breakdown */}
          {monthlyEarnings.length > 0 ? (
            <div className="space-y-2">
              <h3 className="text-sm font-semibold">Monthly Breakdown</h3>
              <div className="space-y-2 max-h-80 overflow-y-auto">
                {monthlyEarnings.map((earning) => (
                  <div
                    key={`${earning.year}-${earning.month}`}
                    className="flex items-center justify-between p-3 border rounded-lg hover:bg-accent/50 transition-colors"
                  >
                    <div>
                      <p className="font-medium">
                        {MONTH_NAMES[earning.month - 1]} {earning.year}
                      </p>
                      <p className="text-sm text-muted-foreground">
                        {earning.bookingCount} {earning.bookingCount === 1 ? 'booking' : 'bookings'}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="font-bold text-primary">€{earning.totalEarnings.toFixed(2)}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <div className="text-center py-8 text-muted-foreground">
              <p>No earnings recorded yet.</p>
              <p className="text-sm mt-2">Earnings will appear once rentals are completed.</p>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
};
