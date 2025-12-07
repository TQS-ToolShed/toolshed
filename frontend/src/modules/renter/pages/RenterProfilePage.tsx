import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '@/modules/auth/context/AuthContext';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { getBookingsForRenter, type BookingResponse } from '../api/bookings-api';
import { RenterNavbar } from '../components/RenterNavbar';
import { BackToDashboardButton } from '../components/BackToDashboardButton';
import { RenterBookingList } from '../components/RenterBookingList';


export const RenterProfilePage = () => {
  const { user } = useAuth();
  const [bookings, setBookings] = useState<BookingResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      if (!user?.id) {
        setError('You need to be logged in to view your profile.');
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
            <p className="text-sm text-muted-foreground">Your account</p>
            <h1 className="text-3xl font-bold">Profile</h1>
          </div>
          <BackToDashboardButton />
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <Card className="lg:col-span-1">
            <CardHeader>
              <CardTitle>Renter details</CardTitle>
              <CardDescription>Basic info for your account</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3 text-sm">
              <div>
                <p className="text-muted-foreground">Name</p>
                <p className="font-semibold text-foreground">
                  {user?.firstName} {user?.lastName}
                </p>
              </div>
              <div>
                <p className="text-muted-foreground">Email</p>
                <p className="font-semibold text-foreground">{user?.email}</p>
              </div>
              <div>
                <p className="text-muted-foreground">Role</p>
                <p className="font-semibold text-foreground">{user?.role}</p>
              </div>
            </CardContent>
          </Card>

        <RenterBookingList
          title="Active bookings"
          description="Bookings currently in progress"
          bookings={activeBookings}
          isLoading={isLoading}
          error={error}
          emptyLabel="No active bookings today."
          maxHeight="8rem"
        />
        </div>

        <RenterBookingList
          title="Upcoming bookings"
          description="Approved bookings scheduled for the future"
          bookings={futureBookings}
          isLoading={isLoading}
          error={error}
          emptyLabel="No upcoming bookings."
          maxHeight="18rem"
        />

        <RenterBookingList
          title="Sent booking requests"
          description="Requests you have submitted and are awaiting owner response"
          bookings={pendingBookings}
          isLoading={isLoading}
          error={error}
          emptyLabel="No pending booking requests."
          maxHeight="18rem"
        />
      </main>
    </div>
  );
};
