import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import type { BookingResponse } from '../api/bookings-api';
import { handlePayBooking } from '../api/payment-api';

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
  showPayButton?: boolean; // Enable payment button for pending payments
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
}: RenterBookingListProps) => {
  const [payingBookingId, setPayingBookingId] = useState<string | null>(null);
  const [paymentError, setPaymentError] = useState<string | null>(null);

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
      // Note: User will be redirected to Stripe, so this won't execute
    } catch (err) {
      setPaymentError(err instanceof Error ? err.message : 'Payment failed');
      setPayingBookingId(null);
    }
  };

  // Determine if a booking can be paid
  const canPay = (booking: BookingResponse) => {
    return (
      showPayButton &&
      booking.paymentStatus === 'PENDING' &&
      booking.status === 'APPROVED'
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
                
                <div className="flex items-center gap-2">
                  {/* Payment Status Badge */}
                  <span
                    className={`px-2 py-1 rounded-full text-xs font-semibold ${
                      booking.paymentStatus === 'COMPLETED'
                        ? 'bg-green-100 text-green-800'
                        : booking.paymentStatus === 'PENDING'
                        ? 'bg-yellow-100 text-yellow-800'
                        : 'bg-gray-100 text-gray-800'
                    }`}
                  >
                    {booking.paymentStatus === 'COMPLETED' ? '✓ Paid' : 'Payment Pending'}
                  </span>

                  {/* Booking Status Badge */}
                  <span
                    className={`px-2 py-1 rounded-full text-xs font-semibold ${
                      booking.status === 'APPROVED'
                        ? 'bg-emerald-100 text-emerald-800'
                        : booking.status === 'PENDING'
                        ? 'bg-blue-100 text-blue-800'
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
                </div>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
};
