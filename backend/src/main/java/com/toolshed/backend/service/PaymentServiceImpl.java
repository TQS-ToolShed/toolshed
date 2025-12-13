package com.toolshed.backend.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.toolshed.backend.dto.CheckoutSessionResponse;
import com.toolshed.backend.dto.CreateCheckoutSessionRequest;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.enums.DepositStatus;
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
    public CheckoutSessionResponse createCheckoutSession(CreateCheckoutSessionRequest request, String successUrl,
            String cancelUrl) {
        validateBookingForPayment(request.getBookingId());
        Long amountInCents = request.getAmountInCents();

        try {
            SessionCreateParams params = buildSessionParams(request, successUrl, cancelUrl, amountInCents);
            Session session = Session.create(params);

            return CheckoutSessionResponse.builder()
                    .sessionId(session.getId())
                    .checkoutUrl(session.getUrl())
                    .build();
        } catch (StripeException e) {
            throw new PaymentProcessingException("Failed to create checkout session: " + e.getMessage(), e);
        }
    }

    @Override
    public CheckoutSessionResponse createDepositCheckoutSession(UUID bookingId, String successUrl, String cancelUrl) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(BOOKING_NOT_FOUND_MSG + bookingId));

        if (booking.getDepositStatus() != DepositStatus.REQUIRED) {
            throw new DepositNotRequiredException("No deposit required or already paid for booking: " + bookingId);
        }

        Long amountInCents = Math.round(booking.getDepositAmount() * 100);

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(
                            successUrl + "?bookingId=" + bookingId + "&type=deposit&session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl + "?bookingId=" + bookingId + "&type=deposit")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("eur")
                                                    .setUnitAmount(amountInCents)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Caução - " + (booking.getTool() != null
                                                                            ? booking.getTool().getTitle()
                                                                            : "Ferramenta"))
                                                                    .setDescription(
                                                                            "Depósito de segurança para Booking ID: "
                                                                                    + bookingId)
                                                                    .build())
                                                    .build())
                                    .build())
                    .putMetadata(BOOKING_ID_KEY, bookingId.toString())
                    .putMetadata("type", "deposit")
                    .build();

            Session session = Session.create(params);

            return CheckoutSessionResponse.builder()
                    .sessionId(session.getId())
                    .checkoutUrl(session.getUrl())
                    .build();
        } catch (StripeException e) {
            throw new PaymentProcessingException("Failed to create deposit checkout session: " + e.getMessage(), e);
        }
    }

    @Override
    public Booking markBookingAsPaid(UUID bookingId) {
        return updatePaymentStatus(bookingId, PaymentStatus.COMPLETED);
    }

    @Override
    public Booking markDepositAsPaid(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(BOOKING_NOT_FOUND_MSG + bookingId));

        if (booking.getDepositStatus() != DepositStatus.REQUIRED) {
            throw new DepositNotRequiredException("No deposit required or already paid for booking: " + bookingId);
        }

        booking.setDepositStatus(DepositStatus.PAID);
        booking.setDepositPaidAt(LocalDateTime.now());
        return bookingRepository.save(booking);
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

    private SessionCreateParams buildSessionParams(CreateCheckoutSessionRequest request,
            String successUrl, String cancelUrl, Long amountInCents) {
        return SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(
                        successUrl + "?bookingId=" + request.getBookingId() + "&session_id={CHECKOUT_SESSION_ID}")
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

    public static class BookingNotFoundException extends RuntimeException {
        public BookingNotFoundException(String message) {
            super(message);
        }
    }

    public static class PaymentAlreadyCompletedException extends RuntimeException {
        public PaymentAlreadyCompletedException(String message) {
            super(message);
        }
    }

    public static class PaymentProcessingException extends RuntimeException {
        public PaymentProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DepositNotRequiredException extends RuntimeException {
        public DepositNotRequiredException(String message) {
            super(message);
        }
    }
}
