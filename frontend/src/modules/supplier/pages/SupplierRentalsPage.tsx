import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '@/modules/auth/context/AuthContext';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { getBookingsForOwner, type SupplierBookingRequest } from '../api/booking-requests-api';
import { SupplierNavbar } from '../components/SupplierNavbar';

const formatDate = (date: string) => new Date(date).toLocaleDateString();

const normalizeDate = (dateString: string) => {
  const date = new Date(dateString);
  date.setHours(0, 0, 0, 0);
  return date;
};

const daysUntil = (dateString: string, today: Date) => {
  const start = normalizeDate(dateString);
  const diff = Math.ceil((start.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
  return diff;
};

export const SupplierRentalsPage = () => {
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

  const load = useCallback(async () => {
    if (!ownerId) {
      setError('You need to be logged in to view rentals.');
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      const data = await getBookingsForOwner(ownerId);
      setBookings(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load rentals');
    } finally {
      setIsLoading(false);
    }
  }, [ownerId]);

  useEffect(() => {
    load();
  }, [load]);

  const activeRentals = useMemo(
    () =>
      bookings.filter((booking) => {
        if (booking.status !== 'APPROVED') return false;
        const start = normalizeDate(booking.startDate);
        const end = normalizeDate(booking.endDate);
        return start.getTime() <= today.getTime() && end.getTime() >= today.getTime();
      }),
    [bookings, today]
  );

  const futureRentals = useMemo(
    () =>
      bookings.filter((booking) => {
        if (booking.status !== 'APPROVED') return false;
        const start = normalizeDate(booking.startDate);
        return start.getTime() > today.getTime();
      }),
    [bookings, today]
  );

  const renderBookingCard = (booking: SupplierBookingRequest, accent: 'active' | 'future') => (
    <div
      key={booking.id}
      className="rounded-lg border border-border p-4 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="space-y-1">
          <p className="font-semibold text-foreground">{booking.toolTitle || 'Tool rental'}</p>
          <p className="text-sm text-muted-foreground">
            Rented by {booking.renterName || 'Renter'}
          </p>
        </div>
        <span
          className={`px-2 py-1 text-xs font-semibold rounded-full ${
            accent === 'active' ? 'bg-emerald-100 text-emerald-800' : 'bg-blue-100 text-blue-800'
          }`}
        >
          {accent === 'active' ? 'ACTIVE' : 'UPCOMING'}
        </span>
      </div>

      <Separator className="my-3" />

      <div className="flex flex-wrap items-center justify-between gap-2 text-sm">
        <span className="text-muted-foreground">
          {formatDate(booking.startDate)} - {formatDate(booking.endDate)}
        </span>
        {booking.totalPrice && (
          <span className="text-foreground font-semibold">
            €{booking.totalPrice.toFixed(2)}
          </span>
        )}
      </div>
      {accent === 'future' && (
        <p className="mt-2 text-xs text-muted-foreground">
          Starts in {daysUntil(booking.startDate, today)} day(s)
        </p>
      )}
    </div>
  );

  return (
    <div className="min-h-screen bg-background">
      <SupplierNavbar />
      <main className="container mx-auto py-8 px-4">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between mb-6">
          <div>
            <p className="text-sm text-muted-foreground mb-1">Rentals overview</p>
            <h2 className="text-3xl font-bold">Active &amp; Future Rentals</h2>
            <p className="text-muted-foreground">
              Track everything that is currently out and what is booked next.
            </p>
          </div>
          <div className="flex flex-wrap gap-3">
            <Button variant="outline" asChild>
              <Link to="/supplier">Back to dashboard</Link>
            </Button>
            <Button variant="secondary" onClick={load} disabled={isLoading}>
              {isLoading ? 'Refreshing...' : 'Refresh'}
            </Button>
          </div>
        </div>

        {error && (
          <div className="mb-4 rounded-md border border-destructive bg-destructive/10 px-4 py-3 text-sm text-destructive">
            {error}
          </div>
        )}

        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2 items-start min-h-[60vh]">
          <div className="flex flex-col h-full">
            <Card className="h-full flex flex-col">
              <CardHeader>
                <CardTitle>Active rentals today</CardTitle>
                <CardDescription>Approved bookings overlapping today</CardDescription>
              </CardHeader>
              <CardContent className="space-y-3 flex-1">
                {isLoading ? (
                  <p className="text-sm text-muted-foreground">Loading active rentals...</p>
                ) : activeRentals.length === 0 ? (
                  <p className="text-sm text-muted-foreground">No rentals are active today.</p>
                ) : (
                  <div className="space-y-3">
                    {activeRentals.map((booking) => renderBookingCard(booking, 'active'))}
                  </div>
                )}
              </CardContent>
            </Card>
          </div>

          <Card className="h-fit">
            <CardHeader>
              <CardTitle>Future rentals</CardTitle>
              <CardDescription>Upcoming approved bookings</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              {isLoading ? (
                <p className="text-sm text-muted-foreground">Loading future rentals...</p>
              ) : futureRentals.length === 0 ? (
                <p className="text-sm text-muted-foreground">No upcoming rentals scheduled.</p>
              ) : (
                <div className="space-y-3">
                  {futureRentals.map((booking) => renderBookingCard(booking, 'future'))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
  );
};
