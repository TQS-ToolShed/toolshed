import { useEffect, useMemo, useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { getBookingsForTool, type BookingResponse } from '../api/bookings-api';

const formatDate = (date: string) => new Date(date).toLocaleDateString();

interface ToolAvailabilityCardProps {
  toolId: string;
}

export const ToolAvailabilityCard = ({ toolId }: ToolAvailabilityCardProps) => {
  const [bookings, setBookings] = useState<BookingResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      try {
        setIsLoading(true);
        setError(null);
        const data = await getBookingsForTool(toolId);
        setBookings(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load availability');
      } finally {
        setIsLoading(false);
      }
    };
    load();
  }, [toolId]);

  const approvedBookings = useMemo(
    () => bookings.filter((b) => b.status === 'APPROVED'),
    [bookings]
  );

  return (
    <Card>
      <CardHeader>
        <CardTitle>Availability</CardTitle>
        <CardDescription>Dates in which this tool is already booked:</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        {error && (
          <div className="bg-destructive/10 border border-destructive text-destructive px-3 py-2 rounded-md text-sm">
            {error}
          </div>
        )}
        {isLoading ? (
          <p className="text-sm text-muted-foreground">Loading availability...</p>
        ) : approvedBookings.length === 0 ? (
          <p className="text-sm text-muted-foreground">No approved bookings yet.</p>
        ) : (
          <div className="space-y-2">
            {approvedBookings.map((booking) => (
              <div
                key={booking.id}
                className="flex items-center justify-between border border-border rounded-lg px-3 py-2 text-sm"
              >
                <span className="text-muted-foreground">
                  Booked from {formatDate(booking.startDate)} to {formatDate(booking.endDate)}
                </span>
                <span className="px-2 py-1 rounded-full text-xs font-semibold bg-amber-100 text-amber-800">
                  Booked
                </span>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
};
