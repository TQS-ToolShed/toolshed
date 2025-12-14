package com.toolshed.backend.service;

import java.util.List;
import java.util.UUID;

import com.toolshed.backend.dto.CheckoutSessionResponse;
import com.toolshed.backend.dto.CreateCheckoutSessionRequest;
import com.toolshed.backend.dto.MonthlyEarningsResponse;
import com.toolshed.backend.dto.PayoutResponse;
import com.toolshed.backend.dto.WalletResponse;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.enums.PaymentStatus;

/**
 * Service interface for payment operations.
 */
public interface PaymentService {

    /**
     * Creates a Stripe Checkout Session for a booking payment.
     *
     * @param request    The checkout session request containing booking details
     * @param successUrl The URL to redirect to after successful payment
     * @param cancelUrl  The URL to redirect to if payment is cancelled
     * @return CheckoutSessionResponse with session ID and checkout URL
     */
    CheckoutSessionResponse createCheckoutSession(CreateCheckoutSessionRequest request, String successUrl,
            String cancelUrl);

    /**
     * Creates a Stripe Checkout Session for a deposit payment.
     *
     * @param bookingId  The booking ID for the deposit
     * @param successUrl The URL to redirect to after successful payment
     * @param cancelUrl  The URL to redirect to if payment is cancelled
     * @return CheckoutSessionResponse with session ID and checkout URL
     */
    CheckoutSessionResponse createDepositCheckoutSession(UUID bookingId, String successUrl, String cancelUrl);

    /**
     * Marks a booking as paid.
     *
     * @param bookingId The ID of the booking to mark as paid
     * @return The updated booking
     */
    Booking markBookingAsPaid(UUID bookingId);

    /**
     * Marks a booking's deposit as paid.
     *
     * @param bookingId The ID of the booking whose deposit to mark as paid
     * @return The updated booking
     */
    Booking markDepositAsPaid(UUID bookingId);

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
     * @throws IllegalStateException    if booking is already paid
     */
    Booking validateBookingForPayment(UUID bookingId);

    /**
     * Updates the payment status of a booking.
     *
     * @param bookingId The ID of the booking
     * @param status    The new payment status
     * @return The updated booking
     */
    Booking updatePaymentStatus(UUID bookingId, PaymentStatus status);

    // ============ Wallet & Payout Operations ============

    /**
     * Gets the wallet information for an owner.
     *
     * @param ownerId The ID of the owner
     * @return WalletResponse with balance and recent payouts
     */
    WalletResponse getOwnerWallet(UUID ownerId);

    /**
     * Gets the payout history for an owner.
     *
     * @param ownerId The ID of the owner
     * @return List of payout responses
     */
    List<PayoutResponse> getPayoutHistory(UUID ownerId);

    /**
     * Requests a payout for an owner.
     *
     * @param ownerId The ID of the owner
     * @param amount  The amount to payout
     * @return PayoutResponse with payout details
     */
    PayoutResponse requestPayout(UUID ownerId, Double amount);

    /**
     * Gets the monthly earnings for an owner.
     *
     * @param ownerId The ID of the owner
     * @return List of monthly earnings
     */
    List<MonthlyEarningsResponse> getMonthlyEarnings(UUID ownerId);
}
