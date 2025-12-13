import axios from 'axios';
import { API_BASE_URL } from '@/lib/api';

const PAYMENTS_URL = `${API_BASE_URL}/api/payments`;

/**
 * Request body for creating a checkout session
 */
export interface CreateCheckoutSessionRequest {
  bookingId: string;
  amountInCents: number;
  description: string;
}

/**
 * Response from the create checkout session endpoint
 */
export interface CheckoutSessionResponse {
  sessionId: string;
  checkoutUrl: string;
}

/**
 * Booking data needed for payment (simplified from BookingResponse)
 */
export interface PayableBooking {
  id: string;
  totalPrice: number;
  toolTitle?: string;
  paymentStatus: string;
}

/**
 * Creates a Stripe Checkout Session and redirects to the payment page.
 * 
 * Flow:
 * 1. Sends booking details to backend
 * 2. Backend creates Stripe Checkout Session
 * 3. Redirects user to Stripe's hosted payment page
 * 
 * @param booking The booking to pay for
 * @returns Promise that resolves when redirect happens (or rejects on error)
 */
export const handlePayBooking = async (booking: PayableBooking): Promise<void> => {
  if (booking.paymentStatus === 'COMPLETED') {
    throw new Error('This booking is already paid');
  }

  // Convert price to cents (Stripe uses smallest currency unit)
  const amountInCents = Math.round(booking.totalPrice * 100);

  const request: CreateCheckoutSessionRequest = {
    bookingId: booking.id,
    amountInCents: amountInCents,
    description: booking.toolTitle 
      ? `Reserva: ${booking.toolTitle}` 
      : `Reserva #${booking.id.slice(0, 8)}`,
  };

  try {
    // Call backend to create checkout session
    const response = await axios.post<CheckoutSessionResponse>(
      `${PAYMENTS_URL}/create-checkout-session`,
      request
    );

    const { checkoutUrl } = response.data;

    // Option 1: Direct redirect to Stripe (simpler and recommended)
    if (checkoutUrl) {
      window.location.href = checkoutUrl;
      return;
    }

    // If no checkoutUrl, throw error (shouldn't happen with our backend)
    throw new Error('No checkout URL received from server');
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to initiate payment');
    }
    throw error instanceof Error ? error : new Error('Payment initiation failed');
  }
};

/**
 * Creates a Stripe Checkout Session for deposit payment and redirects to Stripe.
 * 
 * @param bookingId The booking ID with required deposit
 * @returns Promise that resolves when redirect happens (or rejects on error)
 */
export const handlePayDeposit = async (bookingId: string): Promise<void> => {
  try {
    const response = await axios.post<CheckoutSessionResponse>(
      `${PAYMENTS_URL}/create-deposit-checkout/${bookingId}`
    );

    const { checkoutUrl } = response.data;

    if (checkoutUrl) {
      window.location.href = checkoutUrl;
      return;
    }

    throw new Error('No checkout URL received from server');
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to initiate deposit payment');
    }
    throw error instanceof Error ? error : new Error('Deposit payment initiation failed');
  }
};

/**
 * Marks a booking as paid after successful Stripe payment.
 * Call this from the success page after the user returns from Stripe.
 * 
 * @param bookingId The booking to mark as paid
 */
export const markBookingAsPaid = async (bookingId: string): Promise<void> => {
  try {
    await axios.post(`${PAYMENTS_URL}/mark-paid/${bookingId}`);
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to update payment status');
    }
    throw new Error('Network error while updating payment status');
  }
};

/**
 * Marks a deposit as paid after successful Stripe payment.
 * 
 * @param bookingId The booking whose deposit to mark as paid
 */
export const markDepositAsPaid = async (bookingId: string): Promise<void> => {
  try {
    await axios.post(`${PAYMENTS_URL}/mark-deposit-paid/${bookingId}`);
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to update deposit status');
    }
    throw new Error('Network error while updating deposit status');
  }
};

/**
 * Gets the payment status of a booking.
 * 
 * @param bookingId The booking to check
 * @returns Payment status information
 */
export const getPaymentStatus = async (bookingId: string): Promise<{
  bookingId: string;
  paymentStatus: string;
  totalPrice: number;
  depositStatus?: string;
  depositAmount?: number;
}> => {
  try {
    const response = await axios.get(`${PAYMENTS_URL}/status/${bookingId}`);
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to get payment status');
    }
    throw new Error('Network error while getting payment status');
  }
};

