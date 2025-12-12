package com.toolshed.backend.boundary;

import com.toolshed.backend.controller.PaymentController;
import com.toolshed.backend.dto.CheckoutSessionResponse;
import com.toolshed.backend.dto.CreateCheckoutSessionRequest;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.enums.PaymentStatus;
import com.toolshed.backend.service.PaymentService;
import com.toolshed.backend.service.PaymentServiceImpl.BookingNotFoundException;
import com.toolshed.backend.service.PaymentServiceImpl.PaymentAlreadyCompletedException;
import com.toolshed.backend.service.PaymentServiceImpl.PaymentProcessingException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private static final String SUCCESS_URL = "http://localhost:5173/payment/success";
    private static final String CANCEL_URL = "http://localhost:5173/payment/cancel";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentController, "successUrl", SUCCESS_URL);
        ReflectionTestUtils.setField(paymentController, "cancelUrl", CANCEL_URL);
    }

    @Nested
    @DisplayName("Create Checkout Session Tests")
    class CreateCheckoutSessionTests {

        @Test
        @DisplayName("Should create checkout session successfully")
        void createCheckoutSession_validRequest_returnsSessionResponse() {
            UUID bookingId = UUID.randomUUID();
            CreateCheckoutSessionRequest request = new CreateCheckoutSessionRequest();
            request.setBookingId(bookingId);
            request.setAmountInCents(5000L);
            request.setDescription("Tool rental payment");

            CheckoutSessionResponse expectedResponse = CheckoutSessionResponse.builder()
                    .sessionId("cs_test_123")
                    .checkoutUrl("https://checkout.stripe.com/pay/cs_test_123")
                    .build();

            when(paymentService.createCheckoutSession(any(CreateCheckoutSessionRequest.class), eq(SUCCESS_URL), eq(CANCEL_URL)))
                    .thenReturn(expectedResponse);

            ResponseEntity<CheckoutSessionResponse> response = paymentController.createCheckoutSession(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getSessionId()).isEqualTo("cs_test_123");
            assertThat(response.getBody().getCheckoutUrl()).isEqualTo("https://checkout.stripe.com/pay/cs_test_123");

            verify(paymentService).createCheckoutSession(any(CreateCheckoutSessionRequest.class), eq(SUCCESS_URL), eq(CANCEL_URL));
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when booking not found")
        void createCheckoutSession_bookingNotFound_throwsNotFound() {
            UUID bookingId = UUID.randomUUID();
            CreateCheckoutSessionRequest request = new CreateCheckoutSessionRequest();
            request.setBookingId(bookingId);
            request.setAmountInCents(5000L);
            request.setDescription("Tool rental payment");

            when(paymentService.createCheckoutSession(any(CreateCheckoutSessionRequest.class), anyString(), anyString()))
                    .thenThrow(new BookingNotFoundException("Booking not found: " + bookingId));

            assertThatThrownBy(() -> paymentController.createCheckoutSession(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("Should throw BAD_REQUEST when booking already paid")
        void createCheckoutSession_alreadyPaid_throwsBadRequest() {
            UUID bookingId = UUID.randomUUID();
            CreateCheckoutSessionRequest request = new CreateCheckoutSessionRequest();
            request.setBookingId(bookingId);
            request.setAmountInCents(5000L);
            request.setDescription("Tool rental payment");

            when(paymentService.createCheckoutSession(any(CreateCheckoutSessionRequest.class), anyString(), anyString()))
                    .thenThrow(new PaymentAlreadyCompletedException("Booking is already paid: " + bookingId));

            assertThatThrownBy(() -> paymentController.createCheckoutSession(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    });
        }

        @Test
        @DisplayName("Should throw INTERNAL_SERVER_ERROR when payment processing fails")
        void createCheckoutSession_processingError_throwsInternalError() {
            UUID bookingId = UUID.randomUUID();
            CreateCheckoutSessionRequest request = new CreateCheckoutSessionRequest();
            request.setBookingId(bookingId);
            request.setAmountInCents(5000L);
            request.setDescription("Tool rental payment");

            when(paymentService.createCheckoutSession(any(CreateCheckoutSessionRequest.class), anyString(), anyString()))
                    .thenThrow(new PaymentProcessingException("Stripe error", new RuntimeException("Connection failed")));

            assertThatThrownBy(() -> paymentController.createCheckoutSession(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        }
    }

    @Nested
    @DisplayName("Mark Booking As Paid Tests")
    class MarkBookingAsPaidTests {

        @Test
        @DisplayName("Should mark booking as paid successfully")
        void markBookingAsPaid_validBookingId_returnsSuccess() {
            UUID bookingId = UUID.randomUUID();
            Booking booking = new Booking();
            booking.setId(bookingId);
            booking.setPaymentStatus(PaymentStatus.COMPLETED);

            when(paymentService.markBookingAsPaid(eq(bookingId))).thenReturn(booking);

            ResponseEntity<Map<String, String>> response = paymentController.markBookingAsPaid(bookingId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("message")).isEqualTo("Booking marked as paid");
            assertThat(response.getBody().get("bookingId")).isEqualTo(bookingId.toString());

            verify(paymentService).markBookingAsPaid(eq(bookingId));
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when booking not found")
        void markBookingAsPaid_bookingNotFound_throwsNotFound() {
            UUID bookingId = UUID.randomUUID();

            when(paymentService.markBookingAsPaid(eq(bookingId)))
                    .thenThrow(new BookingNotFoundException("Booking not found: " + bookingId));

            assertThatThrownBy(() -> paymentController.markBookingAsPaid(bookingId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("Get Payment Status Tests")
    class GetPaymentStatusTests {

        @Test
        @DisplayName("Should return PENDING payment status")
        void getPaymentStatus_pendingPayment_returnsStatus() {
            UUID bookingId = UUID.randomUUID();
            Booking booking = new Booking();
            booking.setId(bookingId);
            booking.setPaymentStatus(PaymentStatus.PENDING);
            booking.setTotalPrice(75.0);

            when(paymentService.getBookingPaymentStatus(eq(bookingId))).thenReturn(booking);

            ResponseEntity<Map<String, Object>> response = paymentController.getPaymentStatus(bookingId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("bookingId")).isEqualTo(bookingId.toString());
            assertThat(response.getBody().get("paymentStatus")).isEqualTo("PENDING");
            assertThat(response.getBody().get("totalPrice")).isEqualTo(75.0);
        }

        @Test
        @DisplayName("Should return COMPLETED payment status")
        void getPaymentStatus_completedPayment_returnsCompleted() {
            UUID bookingId = UUID.randomUUID();
            Booking booking = new Booking();
            booking.setId(bookingId);
            booking.setPaymentStatus(PaymentStatus.COMPLETED);
            booking.setTotalPrice(100.0);

            when(paymentService.getBookingPaymentStatus(eq(bookingId))).thenReturn(booking);

            ResponseEntity<Map<String, Object>> response = paymentController.getPaymentStatus(bookingId);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("paymentStatus")).isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("Should return FAILED payment status")
        void getPaymentStatus_failedPayment_returnsFailed() {
            UUID bookingId = UUID.randomUUID();
            Booking booking = new Booking();
            booking.setId(bookingId);
            booking.setPaymentStatus(PaymentStatus.FAILED);
            booking.setTotalPrice(50.0);

            when(paymentService.getBookingPaymentStatus(eq(bookingId))).thenReturn(booking);

            ResponseEntity<Map<String, Object>> response = paymentController.getPaymentStatus(bookingId);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("paymentStatus")).isEqualTo("FAILED");
        }

        @Test
        @DisplayName("Should return REFUNDED payment status")
        void getPaymentStatus_refundedPayment_returnsRefunded() {
            UUID bookingId = UUID.randomUUID();
            Booking booking = new Booking();
            booking.setId(bookingId);
            booking.setPaymentStatus(PaymentStatus.REFUNDED);
            booking.setTotalPrice(150.0);

            when(paymentService.getBookingPaymentStatus(eq(bookingId))).thenReturn(booking);

            ResponseEntity<Map<String, Object>> response = paymentController.getPaymentStatus(bookingId);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("paymentStatus")).isEqualTo("REFUNDED");
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when booking not found")
        void getPaymentStatus_bookingNotFound_throwsNotFound() {
            UUID bookingId = UUID.randomUUID();

            when(paymentService.getBookingPaymentStatus(eq(bookingId)))
                    .thenThrow(new BookingNotFoundException("Booking not found: " + bookingId));

            assertThatThrownBy(() -> paymentController.getPaymentStatus(bookingId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("Response Format Tests")
    class ResponseFormatTests {

        @Test
        @DisplayName("Mark as paid response should contain message and bookingId")
        void markBookingAsPaid_responseContainsRequiredFields() {
            UUID bookingId = UUID.randomUUID();
            Booking booking = new Booking();
            booking.setId(bookingId);

            when(paymentService.markBookingAsPaid(any())).thenReturn(booking);

            ResponseEntity<Map<String, String>> response = paymentController.markBookingAsPaid(bookingId);

            assertThat(response.getBody()).containsKeys("message", "bookingId");
        }

        @Test
        @DisplayName("Get status response should contain bookingId, paymentStatus and totalPrice")
        void getPaymentStatus_responseContainsRequiredFields() {
            UUID bookingId = UUID.randomUUID();
            Booking booking = new Booking();
            booking.setId(bookingId);
            booking.setPaymentStatus(PaymentStatus.PENDING);
            booking.setTotalPrice(50.0);

            when(paymentService.getBookingPaymentStatus(any())).thenReturn(booking);

            ResponseEntity<Map<String, Object>> response = paymentController.getPaymentStatus(bookingId);

            assertThat(response.getBody()).containsKeys("bookingId", "paymentStatus", "totalPrice");
        }

        @Test
        @DisplayName("Checkout session response should contain sessionId and checkoutUrl")
        void createCheckoutSession_responseContainsRequiredFields() {
            CreateCheckoutSessionRequest request = new CreateCheckoutSessionRequest();
            request.setBookingId(UUID.randomUUID());
            request.setAmountInCents(1000L);
            request.setDescription("Test");

            CheckoutSessionResponse serviceResponse = CheckoutSessionResponse.builder()
                    .sessionId("cs_test")
                    .checkoutUrl("https://test.url")
                    .build();

            when(paymentService.createCheckoutSession(any(), anyString(), anyString()))
                    .thenReturn(serviceResponse);

            ResponseEntity<CheckoutSessionResponse> response = paymentController.createCheckoutSession(request);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getSessionId()).isNotNull();
            assertThat(response.getBody().getCheckoutUrl()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("BookingNotFoundException should map to 404 NOT_FOUND")
        void bookingNotFoundException_mapsTo404() {
            UUID bookingId = UUID.randomUUID();
            String errorMessage = "Booking not found: " + bookingId;

            when(paymentService.markBookingAsPaid(any()))
                    .thenThrow(new BookingNotFoundException(errorMessage));

            assertThatThrownBy(() -> paymentController.markBookingAsPaid(bookingId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(rse.getReason()).contains(errorMessage);
                    });
        }

        @Test
        @DisplayName("PaymentAlreadyCompletedException should map to 400 BAD_REQUEST")
        void paymentAlreadyCompletedException_mapsTo400() {
            UUID bookingId = UUID.randomUUID();
            String errorMessage = "Booking is already paid: " + bookingId;

            CreateCheckoutSessionRequest request = new CreateCheckoutSessionRequest();
            request.setBookingId(bookingId);
            request.setAmountInCents(1000L);
            request.setDescription("Test");

            when(paymentService.createCheckoutSession(any(), anyString(), anyString()))
                    .thenThrow(new PaymentAlreadyCompletedException(errorMessage));

            assertThatThrownBy(() -> paymentController.createCheckoutSession(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(rse.getReason()).contains(errorMessage);
                    });
        }

        @Test
        @DisplayName("PaymentProcessingException should map to 500 INTERNAL_SERVER_ERROR")
        void paymentProcessingException_mapsTo500() {
            String errorMessage = "Failed to create checkout session";

            CreateCheckoutSessionRequest request = new CreateCheckoutSessionRequest();
            request.setBookingId(UUID.randomUUID());
            request.setAmountInCents(1000L);
            request.setDescription("Test");

            when(paymentService.createCheckoutSession(any(), anyString(), anyString()))
                    .thenThrow(new PaymentProcessingException(errorMessage, new RuntimeException()));

            assertThatThrownBy(() -> paymentController.createCheckoutSession(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                        assertThat(rse.getReason()).contains(errorMessage);
                    });
        }
    }
}
