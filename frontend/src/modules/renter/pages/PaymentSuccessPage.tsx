import { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { markBookingAsPaid, getPaymentStatus } from '../api/payment-api';

/**
 * Payment Success Page
 * 
 * Displayed after a successful Stripe checkout.
 * Reads the bookingId from URL params and marks the booking as paid.
 */
export const PaymentSuccessPage = () => {
  const [searchParams] = useSearchParams();
  const bookingId = searchParams.get('bookingId');
  const sessionId = searchParams.get('session_id'); // Stripe session ID (optional)
  
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [paymentInfo, setPaymentInfo] = useState<{
    paymentStatus: string;
    totalPrice: number;
  } | null>(null);

  useEffect(() => {
    const confirmPayment = async () => {
      if (!bookingId) {
        setStatus('error');
        setErrorMessage('No booking ID found in URL');
        return;
      }

      try {
        // Mark the booking as paid in our database
        await markBookingAsPaid(bookingId);
        
        // Get updated payment status
        const statusInfo = await getPaymentStatus(bookingId);
        setPaymentInfo(statusInfo);
        
        setStatus('success');
      } catch (error) {
        setStatus('error');
        setErrorMessage(
          error instanceof Error 
            ? error.message 
            : 'Failed to confirm payment'
        );
      }
    };

    confirmPayment();
  }, [bookingId]);

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-card rounded-lg shadow-lg p-8 text-center">
        {status === 'loading' && (
          <>
            <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-primary/10 flex items-center justify-center">
              <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
            </div>
            <h1 className="text-2xl font-bold mb-2">Confirming Payment...</h1>
            <p className="text-muted-foreground">Please wait while we verify your payment.</p>
          </>
        )}

        {status === 'success' && (
          <>
            <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-green-100 flex items-center justify-center">
              <svg 
                className="w-8 h-8 text-green-600" 
                fill="none" 
                stroke="currentColor" 
                viewBox="0 0 24 24"
              >
                <path 
                  strokeLinecap="round" 
                  strokeLinejoin="round" 
                  strokeWidth={2} 
                  d="M5 13l4 4L19 7" 
                />
              </svg>
            </div>
            <h1 className="text-2xl font-bold text-green-600 mb-2">
              Payment Successful!
            </h1>
            <p className="text-muted-foreground mb-4">
              Your payment has been processed successfully.
            </p>
            
            {paymentInfo && (
              <div className="bg-muted/50 rounded-lg p-4 mb-6 text-left">
                <p className="text-sm text-muted-foreground">
                  <span className="font-medium">Booking ID:</span>{' '}
                  {bookingId?.slice(0, 8)}...
                </p>
                <p className="text-sm text-muted-foreground">
                  <span className="font-medium">Amount:</span>{' '}
                  €{paymentInfo.totalPrice.toFixed(2)}
                </p>
                <p className="text-sm text-muted-foreground">
                  <span className="font-medium">Status:</span>{' '}
                  <span className="text-green-600 font-medium">
                    {paymentInfo.paymentStatus}
                  </span>
                </p>
                {sessionId && (
                  <p className="text-xs text-muted-foreground mt-2">
                    Stripe Session: {sessionId.slice(0, 20)}...
                  </p>
                )}
              </div>
            )}

            <Link
              to="/renter/my-bookings"
              className="inline-flex items-center justify-center px-6 py-3 bg-primary text-primary-foreground rounded-lg font-medium hover:bg-primary/90 transition-colors"
            >
              View My Bookings
            </Link>
          </>
        )}

        {status === 'error' && (
          <>
            <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-red-100 flex items-center justify-center">
              <svg 
                className="w-8 h-8 text-red-600" 
                fill="none" 
                stroke="currentColor" 
                viewBox="0 0 24 24"
              >
                <path 
                  strokeLinecap="round" 
                  strokeLinejoin="round" 
                  strokeWidth={2} 
                  d="M6 18L18 6M6 6l12 12" 
                />
              </svg>
            </div>
            <h1 className="text-2xl font-bold text-red-600 mb-2">
              Payment Confirmation Failed
            </h1>
            <p className="text-muted-foreground mb-4">
              {errorMessage || 'There was an issue confirming your payment.'}
            </p>
            <p className="text-sm text-muted-foreground mb-6">
              Don't worry - if your payment went through, it will be reflected shortly.
              Please contact support if the issue persists.
            </p>
            <Link
              to="/renter/my-bookings"
              className="inline-flex items-center justify-center px-6 py-3 bg-primary text-primary-foreground rounded-lg font-medium hover:bg-primary/90 transition-colors"
            >
              Back to Bookings
            </Link>
          </>
        )}
      </div>
    </div>
  );
};
