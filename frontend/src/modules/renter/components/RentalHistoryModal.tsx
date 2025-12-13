import { useEffect, useMemo, useState } from 'react';
import { getBookingsForRenter, type BookingResponse } from '../api/bookings-api';
import { useAuth } from '@/modules/auth/context/AuthContext';

interface RentalHistoryModalProps {
  open: boolean;
  onClose: () => void;
}

export const RentalHistoryModal = ({ open, onClose }: RentalHistoryModalProps) => {
  const { user } = useAuth();
  const [bookings, setBookings] = useState<BookingResponse[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      if (!open || !user?.id) return;
      try {
        setIsLoading(true);
        setError(null);
        const data = await getBookingsForRenter(user.id);
        setBookings(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load history');
      } finally {
        setIsLoading(false);
      }
    };
    load();
  }, [open, user?.id]);

  const pastBookings = useMemo(
    () =>
      bookings
        .filter((booking) => booking.status === 'COMPLETED' || booking.status === 'CANCELLED')
        .sort((a, b) => new Date(b.startDate).getTime() - new Date(a.startDate).getTime()),
    [bookings]
  );

  const statusStyles: Record<string, string> = {
    COMPLETED: 'bg-emerald-100 text-emerald-800',
    CANCELLED: 'bg-amber-100 text-amber-800',
    REJECTED: 'bg-red-100 text-red-800',
    APPROVED: 'bg-blue-100 text-blue-800',
    PENDING: 'bg-gray-100 text-gray-800'
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm px-4">
      <div className="bg-background border border-border rounded-xl shadow-xl w-full max-w-3xl max-h-[80vh] overflow-hidden flex flex-col">
        <div className="flex items-center justify-between px-5 py-4 border-b border-border">
          <div>
            <p className="text-sm text-muted-foreground">Rental history</p>
            <h2 className="text-xl font-semibold">Past bookings</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-sm text-muted-foreground hover:text-foreground"
          >
            Close
          </button>
        </div>
        <div className="p-5 flex-1 overflow-y-auto space-y-3">
          {error && (
            <div className="bg-destructive/10 border border-destructive text-destructive px-3 py-2 rounded-md text-sm">
              {error}
            </div>
          )}
          {isLoading ? (
            <p className="text-sm text-muted-foreground">Loading history...</p>
          ) : pastBookings.length === 0 ? (
            <p className="text-sm text-muted-foreground">No past bookings yet.</p>
          ) : (
            <div className="space-y-3 max-h-[60vh] overflow-y-auto pr-1">
              {pastBookings.map((booking) => {
                const badgeStyle = statusStyles[booking.status] || 'bg-gray-100 text-gray-800';
                return (
                  <div
                    key={booking.id}
                    className="border border-border rounded-lg p-3 flex items-center justify-between gap-3"
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <p className="font-semibold text-foreground">
                          {booking.toolTitle || `Tool ${booking.toolId.slice(0, 6)}`}
                        </p>
                        <span
                          className={`px-2 py-1 rounded-full text-xs font-semibold ${badgeStyle}`}
                        >
                          {booking.status}
                        </span>
                      </div>
                      <p className="text-sm text-muted-foreground">
                        {new Date(booking.startDate).toLocaleDateString()} -{' '}
                        {new Date(booking.endDate).toLocaleDateString()}
                      </p>
                      <div className="text-xs text-muted-foreground flex flex-wrap gap-3">
                        <span className="text-foreground font-semibold">
                          €{booking.totalPrice.toFixed(2)}
                        </span>
                        <span>Payment: {booking.paymentStatus || 'N/A'}</span>
                        <span>ID: {booking.id.slice(0, 8)}</span>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
