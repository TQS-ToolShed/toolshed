package com.toolshed.backend.controller;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.toolshed.backend.dto.CheckoutSessionResponse;
import com.toolshed.backend.dto.CreateCheckoutSessionRequest;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.enums.PaymentStatus;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Stripe payment operations.
 * Handles creation of Checkout Sessions and payment status updates.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final BookingRepository bookingRepository;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    public PaymentController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    /**
     * Creates a Stripe Checkout Session for a booking payment.
     * 
     * Flow:
     * 1. Validates the request
     * 2. Creates a Stripe Checkout Session with the payment details
     * 3. Returns the session ID and checkout URL to the frontend
     * 4. Frontend redirects user to Stripe's hosted checkout page
     * 
     * @param request Contains bookingId, amountInCents, and description
     * @return CheckoutSessionResponse with sessionId and checkoutUrl
     */
    @PostMapping("/create-checkout-session")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            @Valid @RequestBody CreateCheckoutSessionRequest request) {
        
        // Verify the booking exists (optional but recommended)
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found: " + request.getBookingId()));

        // Check if booking is already paid
        if (booking.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking is already paid");
        }

        // In production, you should get the amount from the booking, not trust the client:
        // Long amountInCents = Math.round(booking.getTotalPrice() * 100);
        // For demo purposes, we use the client-provided amount:
        Long amountInCents = request.getAmountInCents();

        try {
            // Build the Stripe Checkout Session
            SessionCreateParams params = SessionCreateParams.builder()
                    // Payment mode (one-time payment)
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    // Where to redirect after successful payment
                    .setSuccessUrl(successUrl + "?bookingId=" + request.getBookingId() + "&session_id={CHECKOUT_SESSION_ID}")
                    // Where to redirect if user cancels
                    .setCancelUrl(cancelUrl + "?bookingId=" + request.getBookingId())
                    // Add the line item (what the user is paying for)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("eur")
                                                    .setUnitAmount(amountInCents)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(request.getDescription())
                                                                    .setDescription("Booking ID: " + request.getBookingId())
                                                                    .build())
                                                    .build())
                                    .build())
                    // Store booking ID in metadata for webhook processing
                    .putMetadata("bookingId", request.getBookingId().toString())
                    .build();

            // Create the session with Stripe
            Session session = Session.create(params);

            // Return the session info to frontend
            CheckoutSessionResponse response = CheckoutSessionResponse.builder()
                    .sessionId(session.getId())
                    .checkoutUrl(session.getUrl())
                    .build();

            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to create checkout session: " + e.getMessage());
        }
    }

    /**
     * Marks a booking as paid after successful Stripe payment.
     * 
     * This endpoint should be called by the frontend after the user
     * returns from a successful Stripe checkout.
     * 
     * Note: In production, you should verify the payment with Stripe
     * before marking as paid, or use webhooks for reliability.
     * 
     * @param bookingId The booking to mark as paid
     * @return Success message
     */
    @PostMapping("/mark-paid/{bookingId}")
    public ResponseEntity<Map<String, String>> markBookingAsPaid(@PathVariable UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found: " + bookingId));

        // Update payment status
        booking.setPaymentStatus(PaymentStatus.COMPLETED);
        bookingRepository.save(booking);

        return ResponseEntity.ok(Map.of(
                "message", "Booking marked as paid",
                "bookingId", bookingId.toString()
        ));
    }

    /**
     * Retrieves the payment status of a booking.
     * 
     * @param bookingId The booking to check
     * @return Payment status information
     */
    @GetMapping("/status/{bookingId}")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(@PathVariable UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found: " + bookingId));

        return ResponseEntity.ok(Map.of(
                "bookingId", bookingId.toString(),
                "paymentStatus", booking.getPaymentStatus().name(),
                "totalPrice", booking.getTotalPrice()
        ));
    }
}
