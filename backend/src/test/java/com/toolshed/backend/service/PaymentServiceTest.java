package com.toolshed.backend.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stripe.exception.StripeException;
import com.stripe.exception.ApiException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import com.toolshed.backend.dto.CheckoutSessionResponse;
import com.toolshed.backend.dto.CreateCheckoutSessionRequest;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.PaymentStatus;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;
import com.toolshed.backend.service.PaymentServiceImpl.BookingNotFoundException;
import com.toolshed.backend.service.PaymentServiceImpl.PaymentAlreadyCompletedException;
import com.toolshed.backend.service.PaymentServiceImpl.PaymentProcessingException;

/**
 * Unit tests for PaymentServiceImpl.
 * Tests payment-related business logic in isolation using mocks.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User renter;
    private User owner;
    private Tool tool;
    private Booking booking;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();

        owner = User.builder()
                .id(UUID.randomUUID())
                .firstName("Owner")
                .lastName("User")
                .email("owner@example.com")
                .password("password123")
                .role(UserRole.SUPPLIER)
                .status(UserStatus.ACTIVE)
                .reputationScore(5.0)
                .build();

        renter = User.builder()
                .id(UUID.randomUUID())
                .firstName("Renter")
                .lastName("User")
                .email("renter@example.com")
                .password("password123")
                .role(UserRole.RENTER)
                .status(UserStatus.ACTIVE)
                .reputationScore(5.0)
                .build();

        tool = Tool.builder()
                .id(UUID.randomUUID())
                .title("Test Tool")
                .description("A test tool")
                .pricePerDay(25.0)
                .location("Test Location")
                .owner(owner)
                .active(true)
                .overallRating(4.5)
                .numRatings(10)
                .build();

        booking = Booking.builder()
                .id(bookingId)
                .tool(tool)
                .renter(renter)
                .owner(owner)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .status(BookingStatus.APPROVED)
                .paymentStatus(PaymentStatus.PENDING)
                .totalPrice(75.0)
                .build();
    }

    @Nested
    @DisplayName("Mark Booking As Paid Tests")
    class MarkBookingAsPaidTests {

        @Test
        @DisplayName("Should mark booking as paid successfully")
        void shouldMarkBookingAsPaidSuccessfully() {
            // Arrange
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Booking result = paymentService.markBookingAsPaid(bookingId);

            // Assert
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
            verify(bookingRepository).findById(bookingId);
            verify(bookingRepository).save(booking);
        }

        @Test
        @DisplayName("Should throw exception when booking not found")
        void shouldThrowExceptionWhenBookingNotFound() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(bookingRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.markBookingAsPaid(nonExistentId))
                    .isInstanceOf(BookingNotFoundException.class)
                    .hasMessageContaining("Booking not found");

            verify(bookingRepository).findById(nonExistentId);
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should update payment status from PENDING to COMPLETED")
        void shouldUpdatePaymentStatusFromPendingToCompleted() {
            // Arrange
            booking.setPaymentStatus(PaymentStatus.PENDING);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Booking result = paymentService.markBookingAsPaid(bookingId);

            // Assert
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should update payment status from FAILED to COMPLETED")
        void shouldUpdatePaymentStatusFromFailedToCompleted() {
            // Arrange
            booking.setPaymentStatus(PaymentStatus.FAILED);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Booking result = paymentService.markBookingAsPaid(bookingId);

            // Assert
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("Get Booking Payment Status Tests")
    class GetBookingPaymentStatusTests {

        @Test
        @DisplayName("Should return booking with PENDING payment status")
        void shouldReturnBookingWithPendingStatus() {
            // Arrange
            booking.setPaymentStatus(PaymentStatus.PENDING);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            // Act
            Booking result = paymentService.getBookingPaymentStatus(bookingId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(bookingId);
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
            verify(bookingRepository).findById(bookingId);
        }

        @Test
        @DisplayName("Should return booking with COMPLETED payment status")
        void shouldReturnBookingWithCompletedStatus() {
            // Arrange
            booking.setPaymentStatus(PaymentStatus.COMPLETED);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            // Act
            Booking result = paymentService.getBookingPaymentStatus(bookingId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should return booking with FAILED payment status")
        void shouldReturnBookingWithFailedStatus() {
            // Arrange
            booking.setPaymentStatus(PaymentStatus.FAILED);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            // Act
            Booking result = paymentService.getBookingPaymentStatus(bookingId);

            // Assert
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("Should return booking with REFUNDED payment status")
        void shouldReturnBookingWithRefundedStatus() {
            // Arrange
            booking.setPaymentStatus(PaymentStatus.REFUNDED);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            // Act
            Booking result = paymentService.getBookingPaymentStatus(bookingId);

            // Assert
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }

        @Test
        @DisplayName("Should throw exception when booking not found")
        void shouldThrowExceptionWhenBookingNotFound() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(bookingRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.getBookingPaymentStatus(nonExistentId))
                    .isInstanceOf(BookingNotFoundException.class)
                    .hasMessageContaining("Booking not found");
        }

        @Test
        @DisplayName("Should return correct total price")
        void shouldReturnCorrectTotalPrice() {
            // Arrange
            booking.setTotalPrice(150.0);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            // Act
            Booking result = paymentService.getBookingPaymentStatus(bookingId);

            // Assert
            assertThat(result.getTotalPrice()).isEqualTo(150.0);
        }
    }

    @Nested
    @DisplayName("Validate Booking For Payment Tests")
    class ValidateBookingForPaymentTests {

        @Test
        @DisplayName("Should return booking when valid for payment")
        void shouldReturnBookingWhenValidForPayment() {
            // Arrange
            booking.setPaymentStatus(PaymentStatus.PENDING);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            // Act
            Booking result = paymentService.validateBookingForPayment(bookingId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(bookingId);
        }

        @Test
        @DisplayName("Should throw exception when booking not found")
        void shouldThrowExceptionWhenBookingNotFound() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(bookingRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.validateBookingForPayment(nonExistentId))
                    .isInstanceOf(BookingNotFoundException.class)
                    .hasMessageContaining("Booking not found");
        }

        @Test
        @DisplayName("Should throw exception when booking is already paid")
        void shouldThrowExceptionWhenBookingAlreadyPaid() {
            // Arrange
            booking.setPaymentStatus(PaymentStatus.COMPLETED);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            // Act & Assert
            assertThatThrownBy(() -> paymentService.validateBookingForPayment(bookingId))
                    .isInstanceOf(PaymentAlreadyCompletedException.class)
                    .hasMessageContaining("Booking is already paid");
        }

        @Test
        @DisplayName("Should allow payment when status is PENDING")
        void shouldAllowPaymentWhenStatusIsPending() {
            // Arrange
            booking.setPaymentStatus(PaymentStatus.PENDING);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            // Act
            Booking result = paymentService.validateBookingForPayment(bookingId);

            // Assert
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("Should allow payment when status is FAILED")
        void shouldAllowPaymentWhenStatusIsFailed() {
            // Arrange
            booking.setPaymentStatus(PaymentStatus.FAILED);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            // Act
            Booking result = paymentService.validateBookingForPayment(bookingId);

            // Assert
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("Should allow payment when status is REFUNDED")
        void shouldAllowPaymentWhenStatusIsRefunded() {
            // Arrange
            booking.setPaymentStatus(PaymentStatus.REFUNDED);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            // Act
            Booking result = paymentService.validateBookingForPayment(bookingId);

            // Assert
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }
    }

    @Nested
    @DisplayName("Update Payment Status Tests")
    class UpdatePaymentStatusTests {

        @Test
        @DisplayName("Should update payment status to COMPLETED")
        void shouldUpdatePaymentStatusToCompleted() {
            // Arrange
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Booking result = paymentService.updatePaymentStatus(bookingId, PaymentStatus.COMPLETED);

            // Assert
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
            verify(bookingRepository).save(booking);
        }

        @Test
        @DisplayName("Should update payment status to FAILED")
        void shouldUpdatePaymentStatusToFailed() {
            // Arrange
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Booking result = paymentService.updatePaymentStatus(bookingId, PaymentStatus.FAILED);

            // Assert
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("Should update payment status to REFUNDED")
        void shouldUpdatePaymentStatusToRefunded() {
            // Arrange
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Booking result = paymentService.updatePaymentStatus(bookingId, PaymentStatus.REFUNDED);

            // Assert
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }

        @Test
        @DisplayName("Should update payment status to PENDING")
        void shouldUpdatePaymentStatusToPending() {
            // Arrange
            booking.setPaymentStatus(PaymentStatus.COMPLETED);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Booking result = paymentService.updatePaymentStatus(bookingId, PaymentStatus.PENDING);

            // Assert
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("Should throw exception when booking not found")
        void shouldThrowExceptionWhenBookingNotFound() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(bookingRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.updatePaymentStatus(nonExistentId, PaymentStatus.COMPLETED))
                    .isInstanceOf(BookingNotFoundException.class)
                    .hasMessageContaining("Booking not found");

            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should call repository save method")
        void shouldCallRepositorySaveMethod() {
            // Arrange
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

            // Act
            paymentService.updatePaymentStatus(bookingId, PaymentStatus.COMPLETED);

            // Assert
            verify(bookingRepository).save(booking);
        }
    }

    @Nested
    @DisplayName("Create Checkout Session Request Validation Tests")
    class CreateCheckoutSessionRequestTests {

        @Test
        @DisplayName("Should validate request with valid booking")
        void shouldValidateRequestWithValidBooking() {
            // Arrange
            CreateCheckoutSessionRequest request = CreateCheckoutSessionRequest.builder()
                    .bookingId(bookingId)
                    .amountInCents(7500L)
                    .description("Tool rental payment")
                    .build();

            booking.setPaymentStatus(PaymentStatus.PENDING);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            // Act
            Booking result = paymentService.validateBookingForPayment(request.getBookingId());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(bookingId);
        }

        @Test
        @DisplayName("Should reject request when booking already completed")
        void shouldRejectRequestWhenBookingAlreadyCompleted() {
            // Arrange
            CreateCheckoutSessionRequest request = CreateCheckoutSessionRequest.builder()
                    .bookingId(bookingId)
                    .amountInCents(7500L)
                    .description("Tool rental payment")
                    .build();

            booking.setPaymentStatus(PaymentStatus.COMPLETED);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            // Act & Assert
            assertThatThrownBy(() -> paymentService.validateBookingForPayment(request.getBookingId()))
                    .isInstanceOf(PaymentAlreadyCompletedException.class);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle booking with null payment status")
        void shouldHandleBookingWithNullPaymentStatus() {
            // Arrange
            booking.setPaymentStatus(null);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            // Act - Should not throw as null != COMPLETED
            Booking result = paymentService.validateBookingForPayment(bookingId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getPaymentStatus()).isNull();
        }

        @Test
        @DisplayName("Should return booking with all related entities")
        void shouldReturnBookingWithAllRelatedEntities() {
            // Arrange
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            // Act
            Booking result = paymentService.getBookingPaymentStatus(bookingId);

            // Assert
            assertThat(result.getTool()).isNotNull();
            assertThat(result.getTool().getTitle()).isEqualTo("Test Tool");
            assertThat(result.getRenter()).isNotNull();
            assertThat(result.getRenter().getEmail()).isEqualTo("renter@example.com");
            assertThat(result.getOwner()).isNotNull();
            assertThat(result.getOwner().getEmail()).isEqualTo("owner@example.com");
        }

        @Test
        @DisplayName("Should preserve booking details after payment status update")
        void shouldPreserveBookingDetailsAfterPaymentStatusUpdate() {
            // Arrange
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Booking result = paymentService.markBookingAsPaid(bookingId);

            // Assert
            assertThat(result.getTotalPrice()).isEqualTo(75.0);
            assertThat(result.getStartDate()).isEqualTo(LocalDate.now().plusDays(1));
            assertThat(result.getEndDate()).isEqualTo(LocalDate.now().plusDays(3));
            assertThat(result.getStatus()).isEqualTo(BookingStatus.APPROVED);
        }
    }

    @Nested
    @DisplayName("Exception Message Tests")
    class ExceptionMessageTests {

        @Test
        @DisplayName("BookingNotFoundException should contain booking ID")
        void bookingNotFoundExceptionShouldContainBookingId() {
            // Arrange
            UUID testId = UUID.randomUUID();
            when(bookingRepository.findById(testId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.getBookingPaymentStatus(testId))
                    .isInstanceOf(BookingNotFoundException.class)
                    .hasMessageContaining(testId.toString());
        }

        @Test
        @DisplayName("PaymentAlreadyCompletedException should contain booking ID")
        void paymentAlreadyCompletedExceptionShouldContainBookingId() {
            // Arrange
            booking.setPaymentStatus(PaymentStatus.COMPLETED);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            // Act & Assert
            assertThatThrownBy(() -> paymentService.validateBookingForPayment(bookingId))
                    .isInstanceOf(PaymentAlreadyCompletedException.class)
                    .hasMessageContaining(bookingId.toString());
        }
    }

    @Nested
    @DisplayName("Create Checkout Session Tests")
    class CreateCheckoutSessionTests {

        private static final String SUCCESS_URL = "http://localhost:5173/payment-success";
        private static final String CANCEL_URL = "http://localhost:5173/payment-cancelled";

        @Test
        @DisplayName("Should create checkout session successfully")
        void shouldCreateCheckoutSessionSuccessfully() {
            // Arrange
            CreateCheckoutSessionRequest request = CreateCheckoutSessionRequest.builder()
                    .bookingId(bookingId)
                    .amountInCents(7500L)
                    .description("Tool rental payment")
                    .build();

            booking.setPaymentStatus(PaymentStatus.PENDING);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            // Mock the Stripe Session static method
            try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
                Session mockSession = org.mockito.Mockito.mock(Session.class);
                when(mockSession.getId()).thenReturn("cs_test_session123");
                when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_test_session123");

                mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                        .thenReturn(mockSession);

                // Act
                CheckoutSessionResponse response = paymentService.createCheckoutSession(request, SUCCESS_URL,
                        CANCEL_URL);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getSessionId()).isEqualTo("cs_test_session123");
                assertThat(response.getCheckoutUrl()).isEqualTo("https://checkout.stripe.com/pay/cs_test_session123");

                verify(bookingRepository).findById(bookingId);
                mockedSession.verify(() -> Session.create(any(SessionCreateParams.class)));
            }
        }

        @Test
        @DisplayName("Should throw BookingNotFoundException when booking does not exist")
        void shouldThrowBookingNotFoundExceptionWhenBookingDoesNotExist() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            CreateCheckoutSessionRequest request = CreateCheckoutSessionRequest.builder()
                    .bookingId(nonExistentId)
                    .amountInCents(7500L)
                    .description("Tool rental payment")
                    .build();

            when(bookingRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.createCheckoutSession(request, SUCCESS_URL, CANCEL_URL))
                    .isInstanceOf(BookingNotFoundException.class)
                    .hasMessageContaining("Booking not found");
        }

        @Test
        @DisplayName("Should throw PaymentAlreadyCompletedException when booking is already paid")
        void shouldThrowPaymentAlreadyCompletedExceptionWhenBookingIsAlreadyPaid() {
            // Arrange
            CreateCheckoutSessionRequest request = CreateCheckoutSessionRequest.builder()
                    .bookingId(bookingId)
                    .amountInCents(7500L)
                    .description("Tool rental payment")
                    .build();

            booking.setPaymentStatus(PaymentStatus.COMPLETED);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            // Act & Assert
            assertThatThrownBy(() -> paymentService.createCheckoutSession(request, SUCCESS_URL, CANCEL_URL))
                    .isInstanceOf(PaymentAlreadyCompletedException.class)
                    .hasMessageContaining("Booking is already paid");
        }

        @Test
        @DisplayName("Should throw PaymentProcessingException when Stripe fails")
        void shouldThrowPaymentProcessingExceptionWhenStripeFails() {
            // Arrange
            CreateCheckoutSessionRequest request = CreateCheckoutSessionRequest.builder()
                    .bookingId(bookingId)
                    .amountInCents(7500L)
                    .description("Tool rental payment")
                    .build();

            booking.setPaymentStatus(PaymentStatus.PENDING);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            // Mock the Stripe Session to throw an exception
            try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
                mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                        .thenThrow(new ApiException("Stripe API error", "req_123", "card_error", 400, null));

                // Act & Assert
                assertThatThrownBy(() -> paymentService.createCheckoutSession(request, SUCCESS_URL, CANCEL_URL))
                        .isInstanceOf(PaymentProcessingException.class)
                        .hasMessageContaining("Failed to create checkout session");
            }
        }

        @Test
        @DisplayName("Should include correct URLs with booking ID in session params")
        void shouldIncludeCorrectUrlsWithBookingIdInSessionParams() {
            // Arrange
            CreateCheckoutSessionRequest request = CreateCheckoutSessionRequest.builder()
                    .bookingId(bookingId)
                    .amountInCents(5000L)
                    .description("Hammer rental - 2 days")
                    .build();

            booking.setPaymentStatus(PaymentStatus.PENDING);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            // Mock the Stripe Session
            try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
                Session mockSession = org.mockito.Mockito.mock(Session.class);
                when(mockSession.getId()).thenReturn("cs_test_xyz");
                when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_test_xyz");

                mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                        .thenAnswer(invocation -> {
                            SessionCreateParams params = invocation.getArgument(0);
                            // Verify the URLs contain the booking ID
                            assertThat(params.getSuccessUrl()).contains(bookingId.toString());
                            assertThat(params.getCancelUrl()).contains(bookingId.toString());
                            assertThat(params.getMode()).isEqualTo(SessionCreateParams.Mode.PAYMENT);
                            return mockSession;
                        });

                // Act
                CheckoutSessionResponse response = paymentService.createCheckoutSession(request, SUCCESS_URL,
                        CANCEL_URL);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getSessionId()).isEqualTo("cs_test_xyz");
            }
        }

        @Test
        @DisplayName("Should allow retry payment for FAILED status")
        void shouldAllowRetryPaymentForFailedStatus() {
            // Arrange
            CreateCheckoutSessionRequest request = CreateCheckoutSessionRequest.builder()
                    .bookingId(bookingId)
                    .amountInCents(3000L)
                    .description("Retry payment for failed booking")
                    .build();

            booking.setPaymentStatus(PaymentStatus.FAILED);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
                Session mockSession = org.mockito.Mockito.mock(Session.class);
                when(mockSession.getId()).thenReturn("cs_retry_123");
                when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_retry_123");

                mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                        .thenReturn(mockSession);

                // Act
                CheckoutSessionResponse response = paymentService.createCheckoutSession(request, SUCCESS_URL,
                        CANCEL_URL);

                // Assert - should succeed since FAILED status allows retry
                assertThat(response).isNotNull();
                assertThat(response.getSessionId()).isEqualTo("cs_retry_123");
            }
        }

        @Test
        @DisplayName("Should set correct amount in cents")
        void shouldSetCorrectAmountInCents() {
            // Arrange
            Long expectedAmountInCents = 15000L; // 150.00 EUR
            CreateCheckoutSessionRequest request = CreateCheckoutSessionRequest.builder()
                    .bookingId(bookingId)
                    .amountInCents(expectedAmountInCents)
                    .description("Power drill rental - 5 days")
                    .build();

            booking.setPaymentStatus(PaymentStatus.PENDING);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
                Session mockSession = org.mockito.Mockito.mock(Session.class);
                when(mockSession.getId()).thenReturn("cs_amount_test");
                when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_amount_test");

                mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                        .thenAnswer(invocation -> {
                            SessionCreateParams params = invocation.getArgument(0);
                            // Verify the line item contains the correct amount
                            assertThat(params.getLineItems()).hasSize(1);
                            SessionCreateParams.LineItem lineItem = params.getLineItems().get(0);
                            assertThat(lineItem.getPriceData().getUnitAmount()).isEqualTo(expectedAmountInCents);
                            assertThat(lineItem.getPriceData().getCurrency()).isEqualTo("eur");
                            return mockSession;
                        });

                // Act
                CheckoutSessionResponse response = paymentService.createCheckoutSession(request, SUCCESS_URL,
                        CANCEL_URL);

                // Assert
                assertThat(response).isNotNull();
            }
        }

        @Test
        @DisplayName("Should include booking metadata in session")
        void shouldIncludeBookingMetadataInSession() {
            // Arrange
            CreateCheckoutSessionRequest request = CreateCheckoutSessionRequest.builder()
                    .bookingId(bookingId)
                    .amountInCents(8000L)
                    .description("Saw rental")
                    .build();

            booking.setPaymentStatus(PaymentStatus.PENDING);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
                Session mockSession = org.mockito.Mockito.mock(Session.class);
                when(mockSession.getId()).thenReturn("cs_metadata_test");
                when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_metadata_test");

                mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                        .thenAnswer(invocation -> {
                            SessionCreateParams params = invocation.getArgument(0);
                            // Verify metadata contains booking ID
                            assertThat(params.getMetadata()).containsKey("bookingId");
                            assertThat(params.getMetadata().get("bookingId")).isEqualTo(bookingId.toString());
                            return mockSession;
                        });

                // Act
                CheckoutSessionResponse response = paymentService.createCheckoutSession(request, SUCCESS_URL,
                        CANCEL_URL);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getSessionId()).isEqualTo("cs_metadata_test");
            }
        }
    }
}
