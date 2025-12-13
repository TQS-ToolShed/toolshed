import { useEffect, useMemo, useState, useCallback } from 'react';
import { useAuth } from '@/modules/auth/context/AuthContext';
import { getBookingsForOwner, type SupplierBookingRequest } from '../api/booking-requests-api';
import { ReviewRenterModal } from './ReviewRenterModal';

interface SupplierRentalHistoryModalProps {
  open: boolean;
  onClose: () => void;
}

const badgeStyles: Record<string, string> = {
  COMPLETED: 'bg-emerald-100 text-emerald-800',
  CANCELLED: 'bg-amber-100 text-amber-800',
  REJECTED: 'bg-red-100 text-red-800',
  APPROVED: 'bg-blue-100 text-blue-800',
  PENDING: 'bg-gray-100 text-gray-800',
};

const conditionStyles: Record<string, string> = {
  OK: 'bg-emerald-100 text-emerald-800',
  USED: 'bg-blue-100 text-blue-800',
  MINOR_DAMAGE: 'bg-amber-100 text-amber-800',
  BROKEN: 'bg-red-100 text-red-800',
  MISSING_PARTS: 'bg-orange-100 text-orange-800',
};

const depositStyles: Record<string, string> = {
  NOT_REQUIRED: 'bg-gray-100 text-gray-600',
  REQUIRED: 'bg-amber-100 text-amber-800',
  PAID: 'bg-emerald-100 text-emerald-800',
};

export const SupplierRentalHistoryModal = ({ open, onClose }: SupplierRentalHistoryModalProps) => {
  const { user } = useAuth();
  const [bookings, setBookings] = useState<SupplierBookingRequest[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reviewModalOpen, setReviewModalOpen] = useState(false);
  const [selectedBooking, setSelectedBooking] = useState<SupplierBookingRequest | null>(null);

  const load = useCallback(async () => {
    if (!open || !user?.id) return;
    try {
      setIsLoading(true);
      setError(null);
      const data = await getBookingsForOwner(user.id);
      setBookings(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load rental history');
    } finally {
      setIsLoading(false);
    }
  }, [open, user?.id]);

  useEffect(() => {
    load();
  }, [load]);

  const handleReviewClick = (booking: SupplierBookingRequest) => {
    setSelectedBooking(booking);
    setReviewModalOpen(true);
  };

  const historyBookings = useMemo(() => {
    return bookings
      .filter((booking) => ['COMPLETED', 'REJECTED', 'CANCELLED'].includes(booking.status))
      .sort((a, b) => new Date(b.startDate).getTime() - new Date(a.startDate).getTime());
  }, [bookings]);

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
            <p className="text-sm text-muted-foreground">Loading rental history...</p>
          ) : historyBookings.length === 0 ? (
            <p className="text-sm text-muted-foreground">No past rentals yet.</p>
          ) : (
            <div className="space-y-3 pr-1">
              {historyBookings.map((booking) => {
                const badgeStyle = badgeStyles[booking.status] || 'bg-gray-100 text-gray-800';
                return (
                  <div
                    key={booking.id}
                    className="border border-border rounded-lg p-3 flex items-start justify-between gap-3"
                  >
                    <div className="space-y-1 flex-1">
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
                        {booking.renterName || 'Renter'} •{' '}
                        {new Date(booking.startDate).toLocaleDateString()} -{' '}
                        {new Date(booking.endDate).toLocaleDateString()}
                      </p>
                      <div className="text-xs text-muted-foreground flex flex-wrap gap-3">
                        {booking.totalPrice !== undefined && (
                          <span className="text-foreground font-semibold">
                            €{booking.totalPrice.toFixed(2)}
                          </span>
                        )}
                        <span>ID: {booking.id.slice(0, 8)}</span>
                      </div>

                      {/* Condition Report Status */}
                      {booking.conditionStatus && (
                        <div className="mt-2 p-2 bg-purple-50/50 rounded-md border border-purple-100">
                          <div className="flex flex-wrap items-center gap-2 text-xs">
                            <span className="font-medium text-xs uppercase tracking-wider text-purple-800">
                              Condition Report:
                            </span>
                            <span
                              className={`px-2 py-0.5 rounded-full font-medium ${conditionStyles[booking.conditionStatus] || 'bg-gray-100 text-gray-800'}`}
                            >
                              {booking.conditionStatus.replace('_', ' ')}
                            </span>
                            {booking.depositStatus && (
                              <span
                                className={`px-2 py-0.5 rounded-full font-medium ${depositStyles[booking.depositStatus] || 'bg-gray-100 text-gray-800'}`}
                              >
                                Deposit: {booking.depositStatus.replace('_', ' ')}
                                {booking.depositAmount && booking.depositStatus !== 'NOT_REQUIRED' && ` (€${booking.depositAmount.toFixed(2)})`}
                              </span>
                            )}
                          </div>
                          {booking.conditionDescription && (
                            <p className="mt-1 text-xs text-purple-900/80 italic">
                              "{booking.conditionDescription}"
                            </p>
                          )}
                          {booking.conditionReportedAt && (
                            <p className="mt-1 text-xs text-muted-foreground">
                              Reported by {booking.conditionReportedByName} on {new Date(booking.conditionReportedAt).toLocaleDateString()}
                            </p>
                          )}
                        </div>
                      )}

                      {booking.review && (
                        <div className="mt-2 p-2 bg-muted/50 rounded-md text-sm">
                          <div className="flex items-center gap-1 mb-1">
                            <span className="font-medium text-xs uppercase tracking-wider text-muted-foreground">Renter Review:</span>
                            <span className="text-yellow-500 text-xs">
                              {'★'.repeat(booking.review.rating)}
                              <span className="text-gray-300">
                                {'★'.repeat(5 - booking.review.rating)}
                              </span>
                            </span>
                          </div>
                          <p className="text-muted-foreground italic text-xs">"{booking.review.comment}"</p>
                        </div>
                      )}
                      {booking.ownerReview && (
                        <div className="mt-2 p-2 bg-blue-50/50 rounded-md text-sm border border-blue-100">
                          <div className="flex items-center gap-1 mb-1">
                            <span className="font-medium text-xs uppercase tracking-wider text-blue-800">Your Review:</span>
                            <span className="text-yellow-500 text-xs">
                              {'★'.repeat(booking.ownerReview.rating)}
                              <span className="text-gray-300">
                                {'★'.repeat(5 - booking.ownerReview.rating)}
                              </span>
                            </span>
                          </div>
                          <p className="text-blue-900/80 italic text-xs">"{booking.ownerReview.comment}"</p>
                        </div>
                      )}
                    </div>
                    
                    <div className="flex flex-col gap-2 pt-1">
                        {booking.status === 'COMPLETED' && (
                            <button 
                                onClick={() => handleReviewClick(booking)}
                                className={`text-xs px-3 py-1.5 rounded-md transition-colors ${
                                    booking.ownerReview 
                                    ? 'border border-input bg-background hover:bg-accent text-foreground' 
                                    : 'bg-primary text-primary-foreground hover:bg-primary/90'
                                }`}
                            >
                                {booking.ownerReview ? 'Edit Review' : 'Review Renter'}
                            </button>
                        )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
      {selectedBooking && (
        <ReviewRenterModal
          open={reviewModalOpen}
          onClose={() => setReviewModalOpen(false)}
          bookingId={selectedBooking.id}
          renterName={selectedBooking.renterName || 'Renter'}
          existingReview={selectedBooking.ownerReview}
          onReviewSubmitted={load}
        />
      )}
    </div>
  );
};
