import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import type { BookingResponse } from '../api/bookings-api';
import { cancelBooking } from '../api/bookings-api';
import { handlePayBooking } from '../api/payment-api';
import { useAuth } from '@/modules/auth/context/AuthContext';

const formatDate = (date: string) => new Date(date).toLocaleDateString();

// Calculate estimated refund percentage based on days until start
const getRefundEstimate = (startDate: string, totalPrice: number) => {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const start = new Date(startDate);
  start.setHours(0, 0, 0, 0);
  const daysUntilStart = Math.ceil((start.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));

  let percentage: number;
  if (daysUntilStart >= 7) {
    percentage = 100;
  } else if (daysUntilStart >= 3) {
    percentage = 50;
  } else if (daysUntilStart >= 1) {
    percentage = 25;
  } else {
    percentage = 0;
  }

  return {
    percentage,
    amount: (totalPrice * percentage) / 100,
    daysUntilStart,
  };
};

interface RenterBookingListProps {
  title: string;
  description: string;
  bookings: BookingResponse[];
  isLoading?: boolean;
  error?: string | null;
  emptyLabel?: string;
  className?: string;
  maxHeight?: string;
  showPayButton?: boolean;
  showCancelButton?: boolean;
  onBookingCancelled?: () => void;
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
  showPayButton = false,
  showCancelButton = false,
  onBookingCancelled,
}: RenterBookingListProps) => {
  const { user } = useAuth();
  const [payingBookingId, setPayingBookingId] = useState<string | null>(null);
  const [cancellingBookingId, setCancellingBookingId] = useState<string | null>(null);
  const [paymentError, setPaymentError] = useState<string | null>(null);
  const [cancelError, setCancelError] = useState<string | null>(null);
  const [cancelSuccess, setCancelSuccess] = useState<string | null>(null);
  const [showCancelConfirm, setShowCancelConfirm] = useState<string | null>(null);

  const handlePay = async (booking: BookingResponse) => {
    setPayingBookingId(booking.id);
    setPaymentError(null);
    
    try {
      await handlePayBooking({
        id: booking.id,
        totalPrice: booking.totalPrice,
        toolTitle: booking.toolTitle,
        paymentStatus: booking.paymentStatus,
      });
    } catch (err) {
      setPaymentError(err instanceof Error ? err.message : 'Payment failed');
      setPayingBookingId(null);
    }
  };

  const handleCancelBooking = async (booking: BookingResponse) => {
    if (!user?.id) return;
    
    setCancellingBookingId(booking.id);
    setCancelError(null);
    setCancelSuccess(null);
    
    try {
      const result = await cancelBooking(booking.id, user.id);
      setCancelSuccess(result.message);
      setShowCancelConfirm(null);
      onBookingCancelled?.();
    } catch (err) {
      setCancelError(err instanceof Error ? err.message : 'Failed to cancel booking');
    } finally {
      setCancellingBookingId(null);
    }
  };

  const canPay = (booking: BookingResponse) => {
    return (
      showPayButton &&
      booking.paymentStatus === 'PENDING' &&
      booking.status === 'APPROVED'
    );
  };

  const canCancel = (booking: BookingResponse) => {
    return (
      showCancelButton &&
      (booking.status === 'PENDING' || booking.status === 'APPROVED')
    );
  };

  return (
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
        {paymentError && (
          <div className="bg-destructive/10 border border-destructive text-destructive px-3 py-2 rounded-md text-sm">
            Payment error: {paymentError}
          </div>
        )}
        {cancelError && (
          <div className="bg-destructive/10 border border-destructive text-destructive px-3 py-2 rounded-md text-sm">
            Cancel error: {cancelError}
          </div>
        )}
        {cancelSuccess && (
          <div className="bg-green-100 border border-green-500 text-green-800 px-3 py-2 rounded-md text-sm">
            {cancelSuccess}
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
            {bookings.map((booking) => {
              const refundEstimate = getRefundEstimate(booking.startDate, booking.totalPrice);
              
              return (
                <div
                  key={booking.id}
                  className="border border-border rounded-lg p-3 space-y-3"
                >
                  <div className="flex items-center justify-between gap-3">
                    <div className="space-y-1 flex-1">
                      <Link
                        to={`/renter/bookings/${booking.toolId}`}
                        className="font-semibold text-foreground hover:underline"
                      >
                        {booking.toolTitle || `Tool ${booking.toolId.slice(0, 6)}`}
                      </Link>
                      <p className="text-sm text-muted-foreground">
                        {formatDate(booking.startDate)} - {formatDate(booking.endDate)}
                      </p>
                      <p className="text-sm font-medium text-foreground">
                        €{booking.totalPrice.toFixed(2)}
                      </p>
                    </div>
                    
                    <div className="flex items-center gap-2 flex-wrap justify-end">
                      {/* Payment Status Badge */}
                      <span
                        className={`px-2 py-1 rounded-full text-xs font-semibold ${
                          booking.paymentStatus === 'COMPLETED'
                            ? 'bg-green-100 text-green-800'
                            : booking.paymentStatus === 'PENDING'
                            ? 'bg-yellow-100 text-yellow-800'
                            : booking.paymentStatus === 'REFUNDED'
                            ? 'bg-purple-100 text-purple-800'
                            : 'bg-gray-100 text-gray-800'
                        }`}
                      >
                        {booking.paymentStatus === 'COMPLETED' ? '✓ Paid' : 
                         booking.paymentStatus === 'REFUNDED' ? '↩ Refunded' : 'Payment Pending'}
                      </span>

                      {/* Booking Status Badge */}
                      <span
                        className={`px-2 py-1 rounded-full text-xs font-semibold ${
                          booking.status === 'APPROVED'
                            ? 'bg-emerald-100 text-emerald-800'
                            : booking.status === 'PENDING'
                            ? 'bg-blue-100 text-blue-800'
                            : booking.status === 'CANCELLED'
                            ? 'bg-red-100 text-red-800'
                            : 'bg-gray-100 text-gray-800'
                        }`}
                      >
                        {booking.status}
                      </span>

                      {/* Pay Button */}
                      {canPay(booking) && (
                        <button
                          onClick={() => handlePay(booking)}
                          disabled={payingBookingId === booking.id}
                          className="px-4 py-2 bg-primary text-primary-foreground rounded-md text-sm font-medium hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                        >
                          {payingBookingId === booking.id ? (
                            <span className="flex items-center gap-2">
                              <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                              Processing...
                            </span>
                          ) : (
                            'Pay Now'
                          )}
                        </button>
                      )}

                      {/* Cancel Button */}
                      {canCancel(booking) && showCancelConfirm !== booking.id && (
                        <button
                          onClick={() => setShowCancelConfirm(booking.id)}
                          className="px-3 py-2 bg-red-100 text-red-700 rounded-md text-sm font-medium hover:bg-red-200 transition-colors"
                        >
                          Cancel
                        </button>
                      )}
                    </div>
                  </div>

                  {/* Cancel Confirmation Dialog */}
                  {showCancelConfirm === booking.id && (
                    <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 space-y-2">
                      <p className="text-sm font-medium text-amber-800">
                        Cancel this booking?
                      </p>
                      <p className="text-xs text-amber-700">
                        Based on our cancellation policy ({refundEstimate.daysUntilStart} days before start):
                        <br />
                        <strong>Refund: {refundEstimate.percentage}% (€{refundEstimate.amount.toFixed(2)})</strong>
                      </p>
                      <div className="flex gap-2">
                        <button
                          onClick={() => handleCancelBooking(booking)}
                          disabled={cancellingBookingId === booking.id}
                          className="px-3 py-1.5 bg-red-600 text-white rounded-md text-sm font-medium hover:bg-red-700 disabled:opacity-50 transition-colors"
                        >
                          {cancellingBookingId === booking.id ? 'Cancelling...' : 'Yes, Cancel'}
                        </button>
                        <button
                          onClick={() => setShowCancelConfirm(null)}
                          className="px-3 py-1.5 bg-gray-200 text-gray-700 rounded-md text-sm font-medium hover:bg-gray-300 transition-colors"
                        >
                          Keep Booking
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </CardContent>
    </Card>
  );
};
