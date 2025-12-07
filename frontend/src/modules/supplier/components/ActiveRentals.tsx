import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '@/modules/auth/context/AuthContext';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { getBookingsForOwner, type SupplierBookingRequest } from '../api/booking-requests-api';

const formatDate = (date: string) => new Date(date).toLocaleDateString();

export const ActiveRentals = () => {
  const { user } = useAuth();
  const ownerId = user?.id;

  const [bookings, setBookings] = useState<SupplierBookingRequest[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const today = useMemo(() => {
    const date = new Date();
    date.setHours(0, 0, 0, 0);
    return date;
  }, []);

  const activeToday = useMemo(
    () =>
      bookings.filter((booking) => {
        if (booking.status !== 'APPROVED') return false;
        const start = new Date(booking.startDate);
        const end = new Date(booking.endDate);
        start.setHours(0, 0, 0, 0);
        end.setHours(0, 0, 0, 0);
        return start.getTime() <= today.getTime() && end.getTime() >= today.getTime();
      }),
    [bookings, today]
  );

  const futureBookings = useMemo(
    () =>
      bookings.filter((booking) => {
        if (booking.status !== 'APPROVED') return false;
        const start = new Date(booking.startDate);
        start.setHours(0, 0, 0, 0);
        return start.getTime() > today.getTime();
      }),
    [bookings, today]
  );

  const previewBookings = useMemo(
    () => [...activeToday, ...futureBookings].slice(0, 3),
    [activeToday, futureBookings]
  );

  const load = useCallback(async () => {
    if (!ownerId) {
      setError('You need to be logged in to view active rentals.');
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      const data = await getBookingsForOwner(ownerId);
      setBookings(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load active rentals');
    } finally {
      setIsLoading(false);
    }
  }, [ownerId]);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <Card>
      <CardHeader>
        <CardTitle>Active &amp; Future rentals</CardTitle>
        <CardDescription>Approved bookings happening today or coming up</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {error && (
          <div className="bg-destructive/10 border border-destructive text-destructive px-3 py-2 rounded-md text-sm">
            {error}
          </div>
        )}
        {isLoading ? (
          <p className="text-sm text-muted-foreground">Loading active rentals...</p>
        ) : (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="rounded-lg border border-border bg-muted/40 p-3">
                <p className="text-sm text-muted-foreground">Active today</p>
                <p className="text-2xl font-semibold text-foreground">{activeToday.length}</p>
              </div>
              <div className="rounded-lg border border-border bg-muted/40 p-3">
                <p className="text-sm text-muted-foreground">Upcoming</p>
                <p className="text-2xl font-semibold text-foreground">{futureBookings.length}</p>
              </div>
            </div>

            <Link to="/supplier/rentals">
              <Button className="w-full">Open rentals page</Button>
            </Link>
          </>
        )}
      </CardContent>
    </Card>
  );
};
