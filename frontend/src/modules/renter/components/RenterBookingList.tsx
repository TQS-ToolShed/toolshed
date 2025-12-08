import { Link } from 'react-router-dom';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import type { BookingResponse } from '../api/bookings-api';

const formatDate = (date: string) => new Date(date).toLocaleDateString();

interface RenterBookingListProps {
  title: string;
  description: string;
  bookings: BookingResponse[];
  isLoading?: boolean;
  error?: string | null;
  emptyLabel?: string;
  className?: string;
  maxHeight?: string;
}

export const RenterBookingList = ({
  title,
  description,
  bookings,
  isLoading = false,
  error,
  emptyLabel = 'No bookings found.',
  className,
  maxHeight,
}: RenterBookingListProps) => (
  <Card className={className}>
    <CardHeader>
      <CardTitle>{title}</CardTitle>
      <CardDescription>{description}</CardDescription>
    </CardHeader>
    <CardContent className="space-y-3">
      {error && (
        <div className="bg-destructive/10 border border-destructive text-destructive px-3 py-2 rounded-md text-sm">
          {error}
        </div>
      )}
      {isLoading ? (
        <p className="text-sm text-muted-foreground">Loading your bookings...</p>
      ) : bookings.length === 0 ? (
        <p className="text-sm text-muted-foreground">{emptyLabel}</p>
      ) : (
        <div
          className="space-y-2"
          style={maxHeight ? { maxHeight, overflowY: 'auto', paddingRight: '0.25rem' } : undefined}
        >
          {bookings.map((booking) => (
            <div
              key={booking.id}
              className="border border-border rounded-lg p-3 flex items-center justify-between gap-3"
            >
              <div className="space-y-1">
                <Link
                  to={`/renter/bookings/${booking.toolId}`}
                  className="font-semibold text-foreground hover:underline"
                >
                  {booking.toolTitle || `Tool ${booking.toolId.slice(0, 6)}`}
                </Link>
                <p className="text-sm text-muted-foreground">
                  {formatDate(booking.startDate)} - {formatDate(booking.endDate)}
                </p>
              </div>
              <span className="px-2 py-1 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800">
                {booking.status}
              </span>
            </div>
          ))}
        </div>
      )}
    </CardContent>
  </Card>
);
