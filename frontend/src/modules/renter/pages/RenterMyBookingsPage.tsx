import { useEffect, useMemo, useState } from 'react';
import { RenterNavbar } from '../components/RenterNavbar';
import { BackToDashboardButton } from '../components/BackToDashboardButton';
import { RenterBookingList } from '../components/RenterBookingList';
import { RentalHistoryModal } from '../components/RentalHistoryModal';
import { getBookingsForRenter, type BookingResponse } from '../api/bookings-api';
import { useAuth } from '@/modules/auth/context/AuthContext';

export const RenterMyBookingsPage = () => {
  const { user } = useAuth();
  const [bookings, setBookings] = useState<BookingResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showHistory, setShowHistory] = useState(false);

  useEffect(() => {
    const load = async () => {
      if (!user?.id) {
        setError('You need to be logged in to view your bookings.');
        setIsLoading(false);
        return;
      }
      try {
        setError(null);
        setIsLoading(true);
        const data = await getBookingsForRenter(user.id);
        setBookings(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load bookings');
      } finally {
        setIsLoading(false);
      }
    };
    load();
  }, [user?.id]);

  const today = useMemo(() => {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    return d;
  }, []);

  const activeBookings = useMemo(
    () =>
      bookings
        .filter((booking) => {
          if (booking.status !== 'APPROVED') return false;
          const start = new Date(booking.startDate);
          const end = new Date(booking.endDate);
          start.setHours(0, 0, 0, 0);
          end.setHours(0, 0, 0, 0);
          return start.getTime() <= today.getTime() && end.getTime() >= today.getTime();
        })
        .sort((a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime()),
    [bookings, today]
  );

  const pendingBookings = useMemo(
    () =>
      bookings
        .filter((booking) => booking.status === 'PENDING')
        .sort((a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime()),
    [bookings]
  );

  const futureBookings = useMemo(
    () =>
      bookings
        .filter((booking) => {
          if (booking.status !== 'APPROVED') return false;
          const start = new Date(booking.startDate);
          start.setHours(0, 0, 0, 0);
          return start.getTime() > today.getTime();
        })
        .sort((a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime()),
    [bookings, today]
  );

  return (
    <div className="min-h-screen bg-background">
      <RenterNavbar />
      <main className="container mx-auto py-8 px-4 space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold">My bookings</h1>
            <p className="text-sm text-muted-foreground">Rentals overview</p>
          </div>
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setShowHistory(true)}
              className="px-4 py-2 rounded-md border border-primary text-primary hover:bg-primary/10 text-sm font-medium"
            >
              Rental history
            </button>
            <BackToDashboardButton />
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <RenterBookingList
            title="Active bookings"
            description="Bookings currently in progress"
            bookings={activeBookings}
            isLoading={isLoading}
            error={error}
            emptyLabel="No active bookings today."
            maxHeight="18rem"
            className="h-full lg:col-span-1"
            showPayButton={true}
          />

          <RenterBookingList
            title="Upcoming bookings"
            description="Approved bookings scheduled for the future"
            bookings={futureBookings}
            isLoading={isLoading}
            error={error}
            emptyLabel="No upcoming bookings."
            maxHeight="18rem"
            className="h-full lg:col-span-1"
            showPayButton={true}
          />
        </div>

        <RenterBookingList
          title="Sent booking requests"
          description="Requests you have submitted and are awaiting owner response"
          bookings={pendingBookings}
          isLoading={isLoading}
          error={error}
          emptyLabel="No pending booking requests."
          maxHeight="18rem"
          className="lg:col-span-2"
        />

        <RentalHistoryModal open={showHistory} onClose={() => setShowHistory(false)} />
      </main>
    </div>
  );
};
