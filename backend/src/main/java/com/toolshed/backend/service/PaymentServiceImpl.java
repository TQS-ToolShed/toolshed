package com.toolshed.backend.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.toolshed.backend.dto.CheckoutSessionResponse;
import com.toolshed.backend.dto.CreateCheckoutSessionRequest;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.enums.PaymentStatus;

/**
 * Implementation of PaymentService for Stripe payment operations.
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final String BOOKING_ID_KEY = "bookingId";
    private static final String BOOKING_NOT_FOUND_MSG = "Booking not found: ";

    private final BookingRepository bookingRepository;

    public PaymentServiceImpl(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public CheckoutSessionResponse createCheckoutSession(CreateCheckoutSessionRequest request, String successUrl, String cancelUrl) {
        // Validate the booking exists and can be paid
        validateBookingForPayment(request.getBookingId());

        Long amountInCents = request.getAmountInCents();

        try {
            // Build the Stripe Checkout Session
            SessionCreateParams params = buildSessionParams(request, successUrl, cancelUrl, amountInCents);

            // Create the session with Stripe
            Session session = Session.create(params);

            // Return the session info
            return CheckoutSessionResponse.builder()
                    .sessionId(session.getId())
                    .checkoutUrl(session.getUrl())
                    .build();

        } catch (StripeException e) {
            throw new PaymentProcessingException("Failed to create checkout session: " + e.getMessage(), e);
        }
    }

    @Override
    public Booking markBookingAsPaid(UUID bookingId) {
        return updatePaymentStatus(bookingId, PaymentStatus.COMPLETED);
    }

    @Override
    public Booking getBookingPaymentStatus(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(BOOKING_NOT_FOUND_MSG + bookingId));
    }

    @Override
    public Booking validateBookingForPayment(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(BOOKING_NOT_FOUND_MSG + bookingId));

        if (booking.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new PaymentAlreadyCompletedException("Booking is already paid: " + bookingId);
        }

        return booking;
    }

    @Override
    public Booking updatePaymentStatus(UUID bookingId, PaymentStatus status) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(BOOKING_NOT_FOUND_MSG + bookingId));

        booking.setPaymentStatus(status);
        return bookingRepository.save(booking);
    }

    /**
     * Builds the Stripe session parameters.
     */
    private SessionCreateParams buildSessionParams(CreateCheckoutSessionRequest request, 
            String successUrl, String cancelUrl, Long amountInCents) {
        return SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl + "?bookingId=" + request.getBookingId() + "&session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl + "?bookingId=" + request.getBookingId())
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
                .putMetadata(BOOKING_ID_KEY, request.getBookingId().toString())
                .build();
    }

    /**
     * Exception thrown when a booking is not found.
     */
    public static class BookingNotFoundException extends RuntimeException {
        public BookingNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when payment is already completed.
     */
    public static class PaymentAlreadyCompletedException extends RuntimeException {
        public PaymentAlreadyCompletedException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when payment processing fails.
     */
    public static class PaymentProcessingException extends RuntimeException {
        public PaymentProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
