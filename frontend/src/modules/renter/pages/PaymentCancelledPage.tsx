import { useSearchParams, Link } from 'react-router-dom';

/**
 * Payment Cancelled Page
 * 
 * Displayed when a user cancels the Stripe checkout process.
 * Provides options to retry or go back to bookings.
 */
export const PaymentCancelledPage = () => {
  const [searchParams] = useSearchParams();
  const bookingId = searchParams.get('bookingId');

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-card rounded-lg shadow-lg p-8 text-center">
        <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-yellow-100 flex items-center justify-center">
          <svg 
            className="w-8 h-8 text-yellow-600" 
            fill="none" 
            stroke="currentColor" 
            viewBox="0 0 24 24"
          >
            <path 
              strokeLinecap="round" 
              strokeLinejoin="round" 
              strokeWidth={2} 
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" 
            />
          </svg>
        </div>
        
        <h1 className="text-2xl font-bold text-yellow-600 mb-2">
          Payment Cancelled
        </h1>
        
        <p className="text-muted-foreground mb-6">
          Your payment was cancelled. No charges have been made to your account.
        </p>

        {bookingId && (
          <p className="text-sm text-muted-foreground mb-6 bg-muted/50 rounded-lg p-3">
            Booking ID: {bookingId.slice(0, 8)}...
            <br />
            <span className="text-xs">
              You can return to your bookings and try again.
            </span>
          </p>
        )}

        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <Link
            to="/renter/my-bookings"
            className="inline-flex items-center justify-center px-6 py-3 bg-primary text-primary-foreground rounded-lg font-medium hover:bg-primary/90 transition-colors"
          >
            Back to My Bookings
          </Link>
          <Link
            to="/renter"
            className="inline-flex items-center justify-center px-6 py-3 border border-input bg-background hover:bg-accent hover:text-accent-foreground rounded-lg font-medium transition-colors"
          >
            Browse Tools
          </Link>
        </div>
      </div>
    </div>
  );
};
