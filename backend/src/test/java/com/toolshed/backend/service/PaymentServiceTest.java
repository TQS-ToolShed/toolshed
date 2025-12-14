package com.toolshed.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.stripe.exception.ApiException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import com.toolshed.backend.dto.CheckoutSessionResponse;
import com.toolshed.backend.dto.CreateCheckoutSessionRequest;
import com.toolshed.backend.dto.PayoutResponse;
import com.toolshed.backend.dto.WalletResponse;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.PayoutRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.entities.Payout;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.PaymentStatus;
import com.toolshed.backend.repository.enums.PayoutStatus;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;
import com.toolshed.backend.service.PaymentServiceImpl.BookingNotFoundException;
import com.toolshed.backend.service.PaymentServiceImpl.InsufficientBalanceException;
import com.toolshed.backend.service.PaymentServiceImpl.InvalidPayoutException;
import com.toolshed.backend.service.PaymentServiceImpl.PaymentAlreadyCompletedException;
import com.toolshed.backend.service.PaymentServiceImpl.PaymentProcessingException;
import com.toolshed.backend.service.PaymentServiceImpl.UserNotFoundException;

/**
 * Unit tests for PaymentServiceImpl.
 * Tests payment-related business logic in isolation using mocks.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PayoutRepository payoutRepository;

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
                .district("Test Location")
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

        @Test
        @DisplayName("Should add security deposit to owner wallet when payment is marked as paid")
        void shouldAddSecurityDepositToOwnerWallet() {
            // Arrange - owner has €100 in wallet, booking costs €50
            owner.setWalletBalance(100.0);
            booking.setTotalPrice(50.0);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            paymentService.markBookingAsPaid(bookingId);

            // Assert - owner should get €50 rental + €8 deposit = €58, total €158
            verify(userRepository).save(owner);
            assertThat(owner.getWalletBalance()).isEqualTo(158.0); // 100 + 50 + 8
        }

        @Test
        @DisplayName("Should set deposit amount on booking when payment is marked as paid")
        void shouldSetDepositAmountOnBooking() {
            // Arrange
            booking.setTotalPrice(50.0);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Booking result = paymentService.markBookingAsPaid(bookingId);

            // Assert - deposit should be €8
            assertThat(result.getDepositAmount()).isEqualTo(8.0);
        }

        @Test
        @DisplayName("Should credit owner wallet with rental price plus deposit")
        void shouldCreditOwnerWalletWithRentalPlusDeposit() {
            // Arrange - owner starts with €0, booking costs €25
            owner.setWalletBalance(0.0);
            booking.setTotalPrice(25.0);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            paymentService.markBookingAsPaid(bookingId);

            // Assert - owner should get €25 rental + €8 deposit = €33
            assertThat(owner.getWalletBalance()).isEqualTo(33.0);
            verify(userRepository).save(owner);
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

    // ===== Deposit Payment Tests =====

    @Nested
    @DisplayName("Mark Deposit As Paid Tests")
    class MarkDepositAsPaidTests {

        @Test
        @DisplayName("Should mark deposit as paid successfully when status is REQUIRED")
        void shouldMarkDepositAsPaidSuccessfully() {
            // Arrange
            booking.setDepositStatus(com.toolshed.backend.repository.enums.DepositStatus.REQUIRED);
            booking.setDepositAmount(50.0);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Booking result = paymentService.markDepositAsPaid(bookingId);

            // Assert
            assertThat(result.getDepositStatus())
                    .isEqualTo(com.toolshed.backend.repository.enums.DepositStatus.PAID);
            assertThat(result.getDepositPaidAt()).isNotNull();
            verify(bookingRepository).save(booking);
        }

        @Test
        @DisplayName("Should throw BookingNotFoundException when booking not found")
        void shouldThrowExceptionWhenBookingNotFound() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(bookingRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.markDepositAsPaid(nonExistentId))
                    .isInstanceOf(BookingNotFoundException.class)
                    .hasMessageContaining("Booking not found");
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw DepositNotRequiredException when deposit status is NOT_REQUIRED")
        void shouldThrowExceptionWhenDepositNotRequired() {
            // Arrange
            booking.setDepositStatus(com.toolshed.backend.repository.enums.DepositStatus.NOT_REQUIRED);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            // Act & Assert
            assertThatThrownBy(() -> paymentService.markDepositAsPaid(bookingId))
                    .isInstanceOf(com.toolshed.backend.service.PaymentServiceImpl.DepositNotRequiredException.class)
                    .hasMessageContaining("No deposit required or already paid");
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw DepositNotRequiredException when deposit already paid")
        void shouldThrowExceptionWhenDepositAlreadyPaid() {
            // Arrange
            booking.setDepositStatus(com.toolshed.backend.repository.enums.DepositStatus.PAID);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            // Act & Assert
            assertThatThrownBy(() -> paymentService.markDepositAsPaid(bookingId))
                    .isInstanceOf(com.toolshed.backend.service.PaymentServiceImpl.DepositNotRequiredException.class)
                    .hasMessageContaining("No deposit required or already paid");
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should set depositPaidAt timestamp when marking as paid")
        void shouldSetDepositPaidAtTimestamp() {
            // Arrange
            booking.setDepositStatus(com.toolshed.backend.repository.enums.DepositStatus.REQUIRED);
            booking.setDepositAmount(50.0);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            Booking result = paymentService.markDepositAsPaid(bookingId);

            // Assert
            assertThat(result.getDepositPaidAt()).isNotNull();
            assertThat(result.getDepositPaidAt()).isBeforeOrEqualTo(java.time.LocalDateTime.now());
        }
    }

    @Nested
    @DisplayName("Create Deposit Checkout Session Tests")
    class CreateDepositCheckoutSessionTests {

        private static final String SUCCESS_URL = "http://localhost:5173/payment/success";
        private static final String CANCEL_URL = "http://localhost:5173/payment/cancelled";

        @Test
        @DisplayName("Should create deposit checkout session successfully")
        void shouldCreateDepositCheckoutSessionSuccessfully() {
            // Arrange
            booking.setDepositStatus(com.toolshed.backend.repository.enums.DepositStatus.REQUIRED);
            booking.setDepositAmount(50.0);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
                Session mockSession = org.mockito.Mockito.mock(Session.class);
                when(mockSession.getId()).thenReturn("cs_deposit_test");
                when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_deposit_test");

                mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                        .thenReturn(mockSession);

                // Act
                CheckoutSessionResponse response = paymentService.createDepositCheckoutSession(
                        bookingId, SUCCESS_URL, CANCEL_URL);

                // Assert
                assertThat(response).isNotNull();
                assertThat(response.getSessionId()).isEqualTo("cs_deposit_test");
                assertThat(response.getCheckoutUrl()).isEqualTo("https://checkout.stripe.com/pay/cs_deposit_test");
            }
        }

        @Test
        @DisplayName("Should throw BookingNotFoundException when booking not found for deposit")
        void shouldThrowExceptionWhenBookingNotFoundForDeposit() {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(bookingRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.createDepositCheckoutSession(
                    nonExistentId, SUCCESS_URL, CANCEL_URL))
                    .isInstanceOf(BookingNotFoundException.class)
                    .hasMessageContaining("Booking not found");
        }

        @Test
        @DisplayName("Should throw DepositNotRequiredException when deposit not required")
        void shouldThrowExceptionWhenDepositNotRequiredForCheckout() {
            // Arrange
            booking.setDepositStatus(com.toolshed.backend.repository.enums.DepositStatus.NOT_REQUIRED);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            // Act & Assert
            assertThatThrownBy(() -> paymentService.createDepositCheckoutSession(
                    bookingId, SUCCESS_URL, CANCEL_URL))
                    .isInstanceOf(com.toolshed.backend.service.PaymentServiceImpl.DepositNotRequiredException.class)
                    .hasMessageContaining("No deposit required or already paid");
        }

        @Test
        @DisplayName("Should throw DepositNotRequiredException when deposit already paid for checkout")
        void shouldThrowExceptionWhenDepositAlreadyPaidForCheckout() {
            // Arrange
            booking.setDepositStatus(com.toolshed.backend.repository.enums.DepositStatus.PAID);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            // Act & Assert
            assertThatThrownBy(() -> paymentService.createDepositCheckoutSession(
                    bookingId, SUCCESS_URL, CANCEL_URL))
                    .isInstanceOf(com.toolshed.backend.service.PaymentServiceImpl.DepositNotRequiredException.class)
                    .hasMessageContaining("No deposit required or already paid");
        }

        @Test
        @DisplayName("Should set correct deposit amount of 50 euros in cents")
        void shouldSetCorrectDepositAmountInCents() {
            // Arrange
            booking.setDepositStatus(com.toolshed.backend.repository.enums.DepositStatus.REQUIRED);
            booking.setDepositAmount(50.0);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
                Session mockSession = org.mockito.Mockito.mock(Session.class);
                when(mockSession.getId()).thenReturn("cs_amount_test");
                when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_amount_test");

                mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                        .thenAnswer(invocation -> {
                            SessionCreateParams params = invocation.getArgument(0);
                            // Verify amount is 5000 cents (€50.00)
                            assertThat(params.getLineItems().get(0).getPriceData().getUnitAmount())
                                    .isEqualTo(5000L);
                            return mockSession;
                        });

                // Act
                paymentService.createDepositCheckoutSession(bookingId, SUCCESS_URL, CANCEL_URL);
            }
        }

        @Test
        @DisplayName("Should include deposit type in metadata")
        void shouldIncludeDepositTypeInMetadata() {
            // Arrange
            booking.setDepositStatus(com.toolshed.backend.repository.enums.DepositStatus.REQUIRED);
            booking.setDepositAmount(50.0);
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
                Session mockSession = org.mockito.Mockito.mock(Session.class);
                when(mockSession.getId()).thenReturn("cs_meta_test");
                when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_meta_test");

                mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                        .thenAnswer(invocation -> {
                            SessionCreateParams params = invocation.getArgument(0);
                            // Verify metadata contains type=deposit
                            assertThat(params.getMetadata()).containsKey("type");
                            assertThat(params.getMetadata().get("type")).isEqualTo("deposit");
                            return mockSession;
                        });

                // Act
                paymentService.createDepositCheckoutSession(bookingId, SUCCESS_URL, CANCEL_URL);
            }
        }
    }

    // ============ Wallet & Payout Tests ============

    @Nested
    @DisplayName("Get Owner Wallet Tests")
    class GetOwnerWalletTests {

        @Test
        @DisplayName("Should return wallet with balance and empty payouts")
        void shouldReturnWalletWithBalance() {
            // Arrange
            owner.setWalletBalance(150.0);
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
            when(payoutRepository.findByOwnerIdOrderByRequestedAtDesc(owner.getId()))
                    .thenReturn(Collections.emptyList());

            // Act
            WalletResponse response = paymentService.getOwnerWallet(owner.getId());

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getBalance()).isEqualTo(150.0);
            assertThat(response.getRecentPayouts()).isEmpty();
        }

        @Test
        @DisplayName("Should return wallet with recent payouts")
        void shouldReturnWalletWithRecentPayouts() {
            // Arrange
            owner.setWalletBalance(50.0);
            Payout payout1 = Payout.builder()
                    .id(UUID.randomUUID())
                    .owner(owner)
                    .amount(100.0)
                    .status(PayoutStatus.COMPLETED)
                    .stripeTransferId("tr_test_123")
                    .requestedAt(LocalDateTime.now().minusDays(1))
                    .completedAt(LocalDateTime.now().minusDays(1))
                    .build();

            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
            when(payoutRepository.findByOwnerIdOrderByRequestedAtDesc(owner.getId()))
                    .thenReturn(Arrays.asList(payout1));

            // Act
            WalletResponse response = paymentService.getOwnerWallet(owner.getId());

            // Assert
            assertThat(response.getBalance()).isEqualTo(50.0);
            assertThat(response.getRecentPayouts()).hasSize(1);
            assertThat(response.getRecentPayouts().get(0).getAmount()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when owner not found")
        void shouldThrowWhenOwnerNotFound() {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.getOwnerWallet(unknownId))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("Should return zero balance when walletBalance is null")
        void shouldReturnZeroBalanceWhenNull() {
            // Arrange
            owner.setWalletBalance(null);
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
            when(payoutRepository.findByOwnerIdOrderByRequestedAtDesc(owner.getId()))
                    .thenReturn(Collections.emptyList());

            // Act
            WalletResponse response = paymentService.getOwnerWallet(owner.getId());

            // Assert
            assertThat(response.getBalance()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Request Payout Tests")
    class RequestPayoutTests {

        @Test
        @DisplayName("Should successfully request payout")
        void shouldSuccessfullyRequestPayout() {
            // Arrange
            owner.setWalletBalance(200.0);
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
            when(payoutRepository.save(any(Payout.class))).thenAnswer(inv -> {
                Payout p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });
            when(userRepository.save(any(User.class))).thenReturn(owner);

            // Act
            PayoutResponse response = paymentService.requestPayout(owner.getId(), 100.0);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getAmount()).isEqualTo(100.0);
            assertThat(response.getStatus()).isEqualTo(PayoutStatus.COMPLETED);
            assertThat(response.getStripeTransferId()).startsWith("tr_simulated_");
            verify(payoutRepository).save(any(Payout.class));
            verify(userRepository).save(owner);
        }

        @Test
        @DisplayName("Should deduct amount from wallet balance")
        void shouldDeductAmountFromBalance() {
            // Arrange
            owner.setWalletBalance(200.0);
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
            when(payoutRepository.save(any(Payout.class))).thenAnswer(inv -> {
                Payout p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            paymentService.requestPayout(owner.getId(), 150.0);

            // Assert - verify the balance was updated
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw InsufficientBalanceException when balance too low")
        void shouldThrowWhenInsufficientBalance() {
            // Arrange
            owner.setWalletBalance(50.0);
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

            // Act & Assert
            assertThatThrownBy(() -> paymentService.requestPayout(owner.getId(), 100.0))
                    .isInstanceOf(InsufficientBalanceException.class)
                    .hasMessageContaining("Insufficient balance");
            verify(payoutRepository, never()).save(any(Payout.class));
        }

        @Test
        @DisplayName("Should throw InvalidPayoutException for zero amount")
        void shouldThrowForZeroAmount() {
            // Arrange
            owner.setWalletBalance(100.0);
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

            // Act & Assert
            assertThatThrownBy(() -> paymentService.requestPayout(owner.getId(), 0.0))
                    .isInstanceOf(InvalidPayoutException.class)
                    .hasMessageContaining("positive");
        }

        @Test
        @DisplayName("Should throw InvalidPayoutException for negative amount")
        void shouldThrowForNegativeAmount() {
            // Arrange
            owner.setWalletBalance(100.0);
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

            // Act & Assert
            assertThatThrownBy(() -> paymentService.requestPayout(owner.getId(), -50.0))
                    .isInstanceOf(InvalidPayoutException.class);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when owner not found")
        void shouldThrowWhenOwnerNotFoundForPayout() {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.requestPayout(unknownId, 100.0))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Payout History Tests")
    class GetPayoutHistoryTests {

        @Test
        @DisplayName("Should return payout history")
        void shouldReturnPayoutHistory() {
            // Arrange
            Payout payout1 = Payout.builder()
                    .id(UUID.randomUUID())
                    .owner(owner)
                    .amount(50.0)
                    .status(PayoutStatus.COMPLETED)
                    .stripeTransferId("tr_1")
                    .requestedAt(LocalDateTime.now().minusDays(2))
                    .completedAt(LocalDateTime.now().minusDays(2))
                    .build();
            Payout payout2 = Payout.builder()
                    .id(UUID.randomUUID())
                    .owner(owner)
                    .amount(75.0)
                    .status(PayoutStatus.COMPLETED)
                    .stripeTransferId("tr_2")
                    .requestedAt(LocalDateTime.now().minusDays(1))
                    .completedAt(LocalDateTime.now().minusDays(1))
                    .build();

            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
            when(payoutRepository.findByOwnerIdOrderByRequestedAtDesc(owner.getId()))
                    .thenReturn(Arrays.asList(payout2, payout1));

            // Act
            List<PayoutResponse> history = paymentService.getPayoutHistory(owner.getId());

            // Assert
            assertThat(history).hasSize(2);
            assertThat(history.get(0).getAmount()).isEqualTo(75.0);
            assertThat(history.get(1).getAmount()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should return empty list when no payouts")
        void shouldReturnEmptyListWhenNoPayouts() {
            // Arrange
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
            when(payoutRepository.findByOwnerIdOrderByRequestedAtDesc(owner.getId()))
                    .thenReturn(Collections.emptyList());

            // Act
            List<PayoutResponse> history = paymentService.getPayoutHistory(owner.getId());

            // Assert
            assertThat(history).isEmpty();
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when owner not found")
        void shouldThrowWhenOwnerNotFoundForHistory() {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> paymentService.getPayoutHistory(unknownId))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Monthly Earnings Tests")
    class GetMonthlyEarningsTests {

        @Test
        @DisplayName("Should get monthly earnings successfully with correct grouping")
        void shouldGetMonthlyEarningsSuccessfully() {
            // Arrange
            UUID ownerId = owner.getId();

            // Booking 1: March 2024, 100.0
            Booking b1 = Booking.builder()
                    .id(UUID.randomUUID())
                    .owner(owner)
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .endDate(LocalDate.of(2024, 3, 10))
                    .totalPrice(100.0)
                    .build();

            // Booking 2: March 2024, 50.0 (Should sum with b1)
            Booking b2 = Booking.builder()
                    .id(UUID.randomUUID())
                    .owner(owner)
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .endDate(LocalDate.of(2024, 3, 20))
                    .totalPrice(50.0)
                    .build();

            // Booking 3: February 2024, 200.0
            Booking b3 = Booking.builder()
                    .id(UUID.randomUUID())
                    .owner(owner)
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .endDate(LocalDate.of(2024, 2, 15))
                    .totalPrice(200.0)
                    .build();

            // Irrelevant bookings
            Booking b4 = Booking.builder()
                    .id(UUID.randomUUID())
                    .owner(renter) // Wrong owner
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .endDate(LocalDate.of(2024, 3, 10))
                    .totalPrice(500.0)
                    .build();

            Booking b5 = Booking.builder()
                    .id(UUID.randomUUID())
                    .owner(owner)
                    .paymentStatus(PaymentStatus.PENDING) // Not completed
                    .endDate(LocalDate.of(2024, 3, 10))
                    .totalPrice(1000.0)
                    .build();

            when(bookingRepository.findAll()).thenReturn(Arrays.asList(b1, b2, b3, b4, b5));
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));

            // Act
            List<com.toolshed.backend.dto.MonthlyEarningsResponse> result = paymentService.getMonthlyEarnings(ownerId);

            // Assert
            assertThat(result).hasSize(2);

            // Check March 2024 (100 + 50 = 150)
            Optional<com.toolshed.backend.dto.MonthlyEarningsResponse> march = result.stream()
                    .filter(r -> r.getMonth().equals("MARCH") && r.getYear() == 2024)
                    .findFirst();
            assertThat(march).isPresent();
            assertThat(march.get().getAmount()).isEqualTo(150.0);

            // Check February 2024 (200)
            Optional<com.toolshed.backend.dto.MonthlyEarningsResponse> feb = result.stream()
                    .filter(r -> r.getMonth().equals("FEBRUARY") && r.getYear() == 2024)
                    .findFirst();
            assertThat(feb).isPresent();
            assertThat(feb.get().getAmount()).isEqualTo(200.0);
        }

        @Test
        @DisplayName("Should return empty list when no earnings")
        void shouldReturnEmptyListWhenNoEarnings() {
            // Arrange
            when(bookingRepository.findAll()).thenReturn(Collections.emptyList());
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

            // Act
            List<com.toolshed.backend.dto.MonthlyEarningsResponse> result = paymentService
                    .getMonthlyEarnings(owner.getId());

            // Assert
            assertThat(result).isEmpty();
        }
    }
}
