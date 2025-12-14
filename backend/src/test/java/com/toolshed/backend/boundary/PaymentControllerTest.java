package com.toolshed.backend.boundary;

import com.toolshed.backend.boundary.PaymentController;
import com.toolshed.backend.dto.CheckoutSessionResponse;
import com.toolshed.backend.dto.CreateCheckoutSessionRequest;
import com.toolshed.backend.dto.PayoutRequest;
import com.toolshed.backend.dto.PayoutResponse;
import com.toolshed.backend.dto.WalletResponse;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.enums.PaymentStatus;
import com.toolshed.backend.repository.enums.PayoutStatus;
import com.toolshed.backend.service.PaymentService;
import com.toolshed.backend.service.PaymentServiceImpl.BookingNotFoundException;
import com.toolshed.backend.service.PaymentServiceImpl.DepositNotRequiredException;
import com.toolshed.backend.service.PaymentServiceImpl.InsufficientBalanceException;
import com.toolshed.backend.service.PaymentServiceImpl.InvalidPayoutException;
import com.toolshed.backend.service.PaymentServiceImpl.PaymentAlreadyCompletedException;
import com.toolshed.backend.service.PaymentServiceImpl.PaymentProcessingException;
import com.toolshed.backend.service.PaymentServiceImpl.UserNotFoundException;

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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

            when(paymentService.createCheckoutSession(any(CreateCheckoutSessionRequest.class), eq(SUCCESS_URL),
                    eq(CANCEL_URL)))
                    .thenReturn(expectedResponse);

            ResponseEntity<CheckoutSessionResponse> response = paymentController.createCheckoutSession(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getSessionId()).isEqualTo("cs_test_123");
            assertThat(response.getBody().getCheckoutUrl()).isEqualTo("https://checkout.stripe.com/pay/cs_test_123");

            verify(paymentService).createCheckoutSession(any(CreateCheckoutSessionRequest.class), eq(SUCCESS_URL),
                    eq(CANCEL_URL));
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when booking not found")
        void createCheckoutSession_bookingNotFound_throwsNotFound() {
            UUID bookingId = UUID.randomUUID();
            CreateCheckoutSessionRequest request = new CreateCheckoutSessionRequest();
            request.setBookingId(bookingId);
            request.setAmountInCents(5000L);
            request.setDescription("Tool rental payment");

            when(paymentService.createCheckoutSession(any(CreateCheckoutSessionRequest.class), anyString(),
                    anyString()))
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

            when(paymentService.createCheckoutSession(any(CreateCheckoutSessionRequest.class), anyString(),
                    anyString()))
                    .thenThrow(new PaymentAlreadyCompletedException("Booking is already paid: " + bookingId));

            assertThatThrownBy(() -> paymentController.createCheckoutSession(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    });
        }

        @Test
        @DisplayName("Should throw INTERNAL_SERVER_ERROR when Stripe fails")
        void createCheckoutSession_stripeError_throwsInternalError() {
            UUID bookingId = UUID.randomUUID();
            CreateCheckoutSessionRequest request = new CreateCheckoutSessionRequest();
            request.setBookingId(bookingId);
            request.setAmountInCents(5000L);
            request.setDescription("Tool rental payment");

            when(paymentService.createCheckoutSession(any(CreateCheckoutSessionRequest.class), anyString(),
                    anyString()))
                    .thenThrow(new PaymentProcessingException("Stripe error", new RuntimeException()));

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
        void markBookingAsPaid_validBooking_returnsSuccess() {
            UUID bookingId = UUID.randomUUID();
            Booking booking = Booking.builder()
                    .id(bookingId)
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .build();

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
        @DisplayName("Should return payment status successfully")
        void getPaymentStatus_validBooking_returnsStatus() {
            UUID bookingId = UUID.randomUUID();
            Booking booking = Booking.builder()
                    .id(bookingId)
                    .paymentStatus(PaymentStatus.PENDING)
                    .totalPrice(100.0)
                    .build();

            when(paymentService.getBookingPaymentStatus(eq(bookingId))).thenReturn(booking);

            ResponseEntity<Map<String, Object>> response = paymentController.getPaymentStatus(bookingId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("bookingId")).isEqualTo(bookingId.toString());
            assertThat(response.getBody().get("paymentStatus")).isEqualTo("PENDING");
            assertThat(response.getBody().get("totalPrice")).isEqualTo(100.0);
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

        @Test
        @DisplayName("Should handle null deposit status")
        void getPaymentStatus_nullDepositStatus_returnsNA() {
            UUID bookingId = UUID.randomUUID();
            Booking booking = Booking.builder()
                    .id(bookingId)
                    .paymentStatus(PaymentStatus.PENDING)
                    .totalPrice(100.0)
                    .depositStatus(null)
                    .depositAmount(null)
                    .build();

            when(paymentService.getBookingPaymentStatus(eq(bookingId))).thenReturn(booking);

            ResponseEntity<Map<String, Object>> response = paymentController.getPaymentStatus(bookingId);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("depositStatus")).isEqualTo("N/A");
            assertThat(response.getBody().get("depositAmount")).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return deposit info when available")
        void getPaymentStatus_withDeposit_returnsDepositInfo() {
            UUID bookingId = UUID.randomUUID();
            Booking booking = Booking.builder()
                    .id(bookingId)
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .totalPrice(100.0)
                    .depositStatus(com.toolshed.backend.repository.enums.DepositStatus.PAID)
                    .depositAmount(50.0)
                    .build();

            when(paymentService.getBookingPaymentStatus(eq(bookingId))).thenReturn(booking);

            ResponseEntity<Map<String, Object>> response = paymentController.getPaymentStatus(bookingId);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("depositStatus")).isEqualTo("PAID");
            assertThat(response.getBody().get("depositAmount")).isEqualTo(50.0);
        }
    }

    @Nested
    @DisplayName("Create Deposit Checkout Tests")
    class CreateDepositCheckoutTests {

        @Test
        @DisplayName("Should create deposit checkout session successfully")
        void createDepositCheckout_validBooking_returnsSessionResponse() {
            UUID bookingId = UUID.randomUUID();
            CheckoutSessionResponse expectedResponse = CheckoutSessionResponse.builder()
                    .sessionId("cs_deposit_123")
                    .checkoutUrl("https://checkout.stripe.com/pay/cs_deposit_123")
                    .build();

            when(paymentService.createDepositCheckoutSession(eq(bookingId), eq(SUCCESS_URL), eq(CANCEL_URL)))
                    .thenReturn(expectedResponse);

            ResponseEntity<CheckoutSessionResponse> response = paymentController
                    .createDepositCheckoutSession(bookingId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getSessionId()).isEqualTo("cs_deposit_123");
            verify(paymentService).createDepositCheckoutSession(eq(bookingId), eq(SUCCESS_URL), eq(CANCEL_URL));
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when booking not found")
        void createDepositCheckout_bookingNotFound_throwsNotFound() {
            UUID bookingId = UUID.randomUUID();

            when(paymentService.createDepositCheckoutSession(eq(bookingId), anyString(), anyString()))
                    .thenThrow(new BookingNotFoundException("Booking not found: " + bookingId));

            assertThatThrownBy(() -> paymentController.createDepositCheckoutSession(bookingId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("Should throw BAD_REQUEST when deposit not required")
        void createDepositCheckout_depositNotRequired_throwsBadRequest() {
            UUID bookingId = UUID.randomUUID();

            when(paymentService.createDepositCheckoutSession(eq(bookingId), anyString(), anyString()))
                    .thenThrow(new DepositNotRequiredException(
                            "No deposit required or already paid for booking: " + bookingId));

            assertThatThrownBy(() -> paymentController.createDepositCheckoutSession(bookingId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    });
        }

        @Test
        @DisplayName("Should throw INTERNAL_SERVER_ERROR when Stripe fails")
        void createDepositCheckout_stripeError_throwsInternalError() {
            UUID bookingId = UUID.randomUUID();

            when(paymentService.createDepositCheckoutSession(eq(bookingId), anyString(), anyString()))
                    .thenThrow(new PaymentProcessingException("Stripe error", new RuntimeException()));

            assertThatThrownBy(() -> paymentController.createDepositCheckoutSession(bookingId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        }
    }

    @Nested
    @DisplayName("Mark Deposit As Paid Tests")
    class MarkDepositAsPaidTests {

        @Test
        @DisplayName("Should mark deposit as paid successfully")
        void markDepositAsPaid_validBooking_returnsSuccess() {
            UUID bookingId = UUID.randomUUID();
            Booking booking = Booking.builder()
                    .id(bookingId)
                    .depositStatus(com.toolshed.backend.repository.enums.DepositStatus.PAID)
                    .build();

            when(paymentService.markDepositAsPaid(eq(bookingId))).thenReturn(booking);

            ResponseEntity<Map<String, String>> response = paymentController.markDepositAsPaid(bookingId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("message")).isEqualTo("Deposit marked as paid");
            assertThat(response.getBody().get("bookingId")).isEqualTo(bookingId.toString());
            verify(paymentService).markDepositAsPaid(eq(bookingId));
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when booking not found")
        void markDepositAsPaid_bookingNotFound_throwsNotFound() {
            UUID bookingId = UUID.randomUUID();

            when(paymentService.markDepositAsPaid(eq(bookingId)))
                    .thenThrow(new BookingNotFoundException("Booking not found: " + bookingId));

            assertThatThrownBy(() -> paymentController.markDepositAsPaid(bookingId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("Should throw BAD_REQUEST when deposit not required or already paid")
        void markDepositAsPaid_depositNotRequired_throwsBadRequest() {
            UUID bookingId = UUID.randomUUID();

            when(paymentService.markDepositAsPaid(eq(bookingId)))
                    .thenThrow(new DepositNotRequiredException(
                            "No deposit required or already paid for booking: " + bookingId));

            assertThatThrownBy(() -> paymentController.markDepositAsPaid(bookingId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    });
        }
    }

    // ============ Wallet & Payout Controller Tests ============

    @Nested
    @DisplayName("Get Owner Wallet Tests")
    class GetOwnerWalletTests {

        @Test
        @DisplayName("Should return wallet successfully")
        void getOwnerWallet_validOwner_returnsWallet() {
            UUID ownerId = UUID.randomUUID();
            WalletResponse walletResponse = WalletResponse.builder()
                    .balance(150.0)
                    .recentPayouts(Collections.emptyList())
                    .build();

            when(paymentService.getOwnerWallet(eq(ownerId))).thenReturn(walletResponse);

            ResponseEntity<WalletResponse> response = paymentController.getOwnerWallet(ownerId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getBalance()).isEqualTo(150.0);
            verify(paymentService).getOwnerWallet(eq(ownerId));
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when owner not found")
        void getOwnerWallet_ownerNotFound_throwsNotFound() {
            UUID ownerId = UUID.randomUUID();

            when(paymentService.getOwnerWallet(eq(ownerId)))
                    .thenThrow(new UserNotFoundException("User not found: " + ownerId));

            assertThatThrownBy(() -> paymentController.getOwnerWallet(ownerId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("Get Payout History Tests")
    class GetPayoutHistoryTests {

        @Test
        @DisplayName("Should return payout history successfully")
        void getPayoutHistory_validOwner_returnsHistory() {
            UUID ownerId = UUID.randomUUID();
            List<PayoutResponse> payouts = Arrays.asList(
                    PayoutResponse.builder()
                            .id(UUID.randomUUID())
                            .amount(50.0)
                            .status(PayoutStatus.COMPLETED)
                            .stripeTransferId("tr_1")
                            .requestedAt(LocalDateTime.now())
                            .build());

            when(paymentService.getPayoutHistory(eq(ownerId))).thenReturn(payouts);

            ResponseEntity<List<PayoutResponse>> response = paymentController.getPayoutHistory(ownerId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).hasSize(1);
            verify(paymentService).getPayoutHistory(eq(ownerId));
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when owner not found")
        void getPayoutHistory_ownerNotFound_throwsNotFound() {
            UUID ownerId = UUID.randomUUID();

            when(paymentService.getPayoutHistory(eq(ownerId)))
                    .thenThrow(new UserNotFoundException("User not found: " + ownerId));

            assertThatThrownBy(() -> paymentController.getPayoutHistory(ownerId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }
    }

    @Nested
    @DisplayName("Request Payout Tests")
    class RequestPayoutTests {

        @Test
        @DisplayName("Should request payout successfully")
        void requestPayout_validRequest_returnsPayout() {
            UUID ownerId = UUID.randomUUID();
            PayoutRequest request = new PayoutRequest(100.0);
            PayoutResponse expectedResponse = PayoutResponse.builder()
                    .id(UUID.randomUUID())
                    .amount(100.0)
                    .status(PayoutStatus.COMPLETED)
                    .stripeTransferId("tr_simulated_abc")
                    .requestedAt(LocalDateTime.now())
                    .completedAt(LocalDateTime.now())
                    .build();

            when(paymentService.requestPayout(eq(ownerId), eq(100.0))).thenReturn(expectedResponse);

            ResponseEntity<PayoutResponse> response = paymentController.requestPayout(ownerId, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getAmount()).isEqualTo(100.0);
            assertThat(response.getBody().getStatus()).isEqualTo(PayoutStatus.COMPLETED);
            verify(paymentService).requestPayout(eq(ownerId), eq(100.0));
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when owner not found")
        void requestPayout_ownerNotFound_throwsNotFound() {
            UUID ownerId = UUID.randomUUID();
            PayoutRequest request = new PayoutRequest(100.0);

            when(paymentService.requestPayout(eq(ownerId), eq(100.0)))
                    .thenThrow(new UserNotFoundException("User not found: " + ownerId));

            assertThatThrownBy(() -> paymentController.requestPayout(ownerId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("Should throw BAD_REQUEST when insufficient balance")
        void requestPayout_insufficientBalance_throwsBadRequest() {
            UUID ownerId = UUID.randomUUID();
            PayoutRequest request = new PayoutRequest(1000.0);

            when(paymentService.requestPayout(eq(ownerId), eq(1000.0)))
                    .thenThrow(new InsufficientBalanceException("Insufficient balance"));

            assertThatThrownBy(() -> paymentController.requestPayout(ownerId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    });
        }

        @Test
        @DisplayName("Should throw BAD_REQUEST for invalid amount")
        void requestPayout_invalidAmount_throwsBadRequest() {
            UUID ownerId = UUID.randomUUID();
            PayoutRequest request = new PayoutRequest(-50.0);

            when(paymentService.requestPayout(eq(ownerId), eq(-50.0)))
                    .thenThrow(new InvalidPayoutException("Payout amount must be positive"));

            assertThatThrownBy(() -> paymentController.requestPayout(ownerId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
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

    @Nested
    @DisplayName("Create Deposit Checkout Session Tests")
    class CreateDepositCheckoutSessionTests {

        @Test
        @DisplayName("Should create deposit checkout session successfully")
        void createDepositCheckoutSession_validBooking_returnsSessionResponse() {
            UUID bookingId = UUID.randomUUID();
            CheckoutSessionResponse expectedResponse = CheckoutSessionResponse.builder()
                    .sessionId("cs_deposit_123")
                    .checkoutUrl("https://checkout.stripe.com/pay/cs_deposit_123")
                    .build();

            when(paymentService.createDepositCheckoutSession(eq(bookingId), eq(SUCCESS_URL), eq(CANCEL_URL)))
                    .thenReturn(expectedResponse);

            ResponseEntity<CheckoutSessionResponse> response = paymentController
                    .createDepositCheckoutSession(bookingId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getSessionId()).isEqualTo("cs_deposit_123");
            verify(paymentService).createDepositCheckoutSession(eq(bookingId), eq(SUCCESS_URL), eq(CANCEL_URL));
        }

        @Test
        @DisplayName("Should throw NOT_FOUND when booking not found")
        void createDepositCheckoutSession_bookingNotFound_throwsNotFound() {
            UUID bookingId = UUID.randomUUID();

            when(paymentService.createDepositCheckoutSession(eq(bookingId), anyString(), anyString()))
                    .thenThrow(new BookingNotFoundException("Booking not found: " + bookingId));

            assertThatThrownBy(() -> paymentController.createDepositCheckoutSession(bookingId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("Should throw BAD_REQUEST when deposit not required")
        void createDepositCheckoutSession_depositNotRequired_throwsBadRequest() {
            UUID bookingId = UUID.randomUUID();

            when(paymentService.createDepositCheckoutSession(eq(bookingId), anyString(), anyString()))
                    .thenThrow(new DepositNotRequiredException(
                            "No deposit required or already paid for booking: " + bookingId));

            assertThatThrownBy(() -> paymentController.createDepositCheckoutSession(bookingId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    });
        }

        @Test
        @DisplayName("Should throw INTERNAL_SERVER_ERROR when payment processing fails")
        void createDepositCheckoutSession_processingError_throwsInternalError() {
            UUID bookingId = UUID.randomUUID();

            when(paymentService.createDepositCheckoutSession(eq(bookingId), anyString(), anyString()))
                    .thenThrow(new PaymentProcessingException("Stripe error", new RuntimeException()));

            assertThatThrownBy(() -> paymentController.createDepositCheckoutSession(bookingId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        }
    }
}
