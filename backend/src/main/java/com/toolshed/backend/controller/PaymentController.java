package com.toolshed.backend.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.toolshed.backend.dto.CheckoutSessionResponse;
import com.toolshed.backend.dto.CreateCheckoutSessionRequest;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.service.PaymentService;
import com.toolshed.backend.service.PaymentServiceImpl.BookingNotFoundException;
import com.toolshed.backend.service.PaymentServiceImpl.DepositNotRequiredException;
import com.toolshed.backend.service.PaymentServiceImpl.PaymentAlreadyCompletedException;
import com.toolshed.backend.service.PaymentServiceImpl.PaymentProcessingException;

import jakarta.validation.Valid;

/**
 * REST Controller for Stripe payment operations.
 * Handles creation of Checkout Sessions and payment status updates.
 */
@RestController
@RequestMapping("/api/payments")
@Profile("!test")
public class PaymentController {

    private static final String BOOKING_ID_KEY = "bookingId";

    private final PaymentService paymentService;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Creates a Stripe Checkout Session for a booking payment.
     */
    @PostMapping("/create-checkout-session")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            @Valid @RequestBody CreateCheckoutSessionRequest request) {
        try {
            CheckoutSessionResponse response = paymentService.createCheckoutSession(request, successUrl, cancelUrl);
            return ResponseEntity.ok(response);
        } catch (BookingNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (PaymentAlreadyCompletedException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (PaymentProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Creates a Stripe Checkout Session for a deposit payment.
     */
    @PostMapping("/create-deposit-checkout/{bookingId}")
    public ResponseEntity<CheckoutSessionResponse> createDepositCheckoutSession(@PathVariable UUID bookingId) {
        try {
            CheckoutSessionResponse response = paymentService.createDepositCheckoutSession(bookingId, successUrl,
                    cancelUrl);
            return ResponseEntity.ok(response);
        } catch (BookingNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (DepositNotRequiredException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (PaymentProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Marks a booking as paid after successful Stripe payment.
     */
    @PostMapping("/mark-paid/{bookingId}")
    public ResponseEntity<Map<String, String>> markBookingAsPaid(@PathVariable UUID bookingId) {
        try {
            paymentService.markBookingAsPaid(bookingId);
            return ResponseEntity.ok(Map.of(
                    "message", "Booking marked as paid",
                    BOOKING_ID_KEY, bookingId.toString()));
        } catch (BookingNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Marks a deposit as paid after successful Stripe payment.
     */
    @PostMapping("/mark-deposit-paid/{bookingId}")
    public ResponseEntity<Map<String, String>> markDepositAsPaid(@PathVariable UUID bookingId) {
        try {
            paymentService.markDepositAsPaid(bookingId);
            return ResponseEntity.ok(Map.of(
                    "message", "Deposit marked as paid",
                    BOOKING_ID_KEY, bookingId.toString()));
        } catch (BookingNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (DepositNotRequiredException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Retrieves the payment status of a booking.
     */
    @GetMapping("/status/{bookingId}")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(@PathVariable UUID bookingId) {
        try {
            Booking booking = paymentService.getBookingPaymentStatus(bookingId);
            return ResponseEntity.ok(Map.of(
                    BOOKING_ID_KEY, bookingId.toString(),
                    "paymentStatus", booking.getPaymentStatus().name(),
                    "totalPrice", booking.getTotalPrice(),
                    "depositStatus", booking.getDepositStatus() != null ? booking.getDepositStatus().name() : "N/A",
                    "depositAmount", booking.getDepositAmount() != null ? booking.getDepositAmount() : 0.0));
        } catch (BookingNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
