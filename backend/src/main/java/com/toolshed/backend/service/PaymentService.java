package com.toolshed.backend.service;

import java.util.UUID;

import com.toolshed.backend.dto.CheckoutSessionResponse;
import com.toolshed.backend.dto.CreateCheckoutSessionRequest;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.enums.PaymentStatus;

/**
 * Service interface for payment operations.
 */
public interface PaymentService {

    /**
     * Creates a Stripe Checkout Session for a booking payment.
     *
     * @param request The checkout session request containing booking details
     * @param successUrl The URL to redirect to after successful payment
     * @param cancelUrl The URL to redirect to if payment is cancelled
     * @return CheckoutSessionResponse with session ID and checkout URL
     */
    CheckoutSessionResponse createCheckoutSession(CreateCheckoutSessionRequest request, String successUrl, String cancelUrl);

    /**
     * Marks a booking as paid.
     *
     * @param bookingId The ID of the booking to mark as paid
     * @return The updated booking
     */
    Booking markBookingAsPaid(UUID bookingId);

    /**
     * Gets the payment status of a booking.
     *
     * @param bookingId The ID of the booking
     * @return The booking with payment status
     */
    Booking getBookingPaymentStatus(UUID bookingId);

    /**
     * Validates that a booking exists and can be paid.
     *
     * @param bookingId The ID of the booking to validate
     * @return The booking if valid
     * @throws IllegalArgumentException if booking not found
     * @throws IllegalStateException if booking is already paid
     */
    Booking validateBookingForPayment(UUID bookingId);

    /**
     * Updates the payment status of a booking.
     *
     * @param bookingId The ID of the booking
     * @param status The new payment status
     * @return The updated booking
     */
    Booking updatePaymentStatus(UUID bookingId, PaymentStatus status);
}
