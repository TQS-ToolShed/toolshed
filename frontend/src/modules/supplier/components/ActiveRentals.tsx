import { useCallback, useEffect, useMemo, useState } from 'react';
import { useAuth } from '@/modules/auth/context/AuthContext';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { getBookingsForOwner, type SupplierBookingRequest } from '../api/booking-requests-api';

const formatDate = (date: string) => new Date(date).toLocaleDateString();

export const ActiveRentals = () => {
  const { user } = useAuth();
  const ownerId = user?.id;

  const [bookings, setBookings] = useState<SupplierBookingRequest[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const activeBookings = useMemo(
    () => bookings.filter((booking) => booking.status === 'APPROVED'),
    [bookings]
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
        <CardTitle>Active rentals</CardTitle>
        <CardDescription>Approved bookings currently out</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        {error && (
          <div className="bg-destructive/10 border border-destructive text-destructive px-3 py-2 rounded-md text-sm">
            {error}
          </div>
        )}
        {isLoading ? (
          <p className="text-sm text-muted-foreground">Loading active rentals...</p>
        ) : activeBookings.length === 0 ? (
          <p className="text-sm text-muted-foreground">No active rentals right now.</p>
        ) : (
          <div className="space-y-2">
            {activeBookings.map((booking) => (
              <div
                key={booking.id}
                className="border border-border rounded-lg p-3 flex items-center justify-between gap-3"
              >
                <div className="space-y-1">
                  <p className="font-semibold text-foreground">
                    {booking.toolTitle || 'Tool rental'}
                  </p>
                  <p className="text-sm text-muted-foreground">
                    {'Rented by '} {booking.renterName || 'Renter'} • {formatDate(booking.startDate)} -{' '}
                    {formatDate(booking.endDate)}
                  </p>
                  {booking.totalPrice && (
                    <p className="text-xs text-muted-foreground">
                      Total: <span className="text-foreground font-semibold">€{booking.totalPrice.toFixed(2)}</span>
                    </p>
                  )}
                </div>
                <span className="px-2 py-1 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800">
                  APPROVED
                </span>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
};
