package com.toolshed.backend.integration;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.PaymentStatus;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;
import com.toolshed.backend.service.PaymentService;
import com.toolshed.backend.service.PaymentServiceImpl.BookingNotFoundException;
import com.toolshed.backend.service.PaymentServiceImpl.PaymentAlreadyCompletedException;

/**
 * Integration tests for PaymentService.
 * Tests the service layer with real database interactions.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PaymentIT {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private User renter;
    private User owner;
    private Tool tool;
    private Booking booking;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        toolRepository.deleteAll();
        userRepository.deleteAll();

        owner = new User();
        owner.setFirstName("Owner");
        owner.setLastName("User");
        owner.setEmail("owner@payment.test");
        owner.setPassword("password");
        owner.setRole(UserRole.SUPPLIER);
        owner.setStatus(UserStatus.ACTIVE);
        owner.setReputationScore(5.0);
        owner = userRepository.save(owner);

        renter = new User();
        renter.setFirstName("Renter");
        renter.setLastName("User");
        renter.setEmail("renter@payment.test");
        renter.setPassword("password");
        renter.setRole(UserRole.RENTER);
        renter.setStatus(UserStatus.ACTIVE);
        renter.setReputationScore(5.0);
        renter = userRepository.save(renter);

        tool = new Tool();
        tool.setTitle("Payment Test Tool");
        tool.setDescription("Tool for payment testing");
        tool.setPricePerDay(25.0);
        tool.setLocation("Test Location");
        tool.setOwner(owner);
        tool.setActive(true);
        tool.setOverallRating(0.0);
        tool.setNumRatings(0);
        tool = toolRepository.save(tool);

        booking = new Booking();
        booking.setTool(tool);
        booking.setRenter(renter);
        booking.setOwner(owner);
        booking.setStartDate(LocalDate.now().plusDays(1));
        booking.setEndDate(LocalDate.now().plusDays(3));
        booking.setStatus(BookingStatus.APPROVED);
        booking.setTotalPrice(75.0);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking = bookingRepository.save(booking);
    }

    @Nested
    @DisplayName("Mark Booking As Paid Tests")
    class MarkBookingAsPaidTests {

        @Test
        @DisplayName("Should mark booking as paid and persist to database")
        void markBookingAsPaid_validBooking_updatesStatusInDatabase() {
            // Act
            Booking result = paymentService.markBookingAsPaid(booking.getId());

            // Assert
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);

            // Verify persistence
            Booking persistedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
            assertThat(persistedBooking.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should throw exception for non-existent booking")
        void markBookingAsPaid_nonExistentBooking_throwsException() {
            UUID nonExistentId = UUID.randomUUID();

            assertThatThrownBy(() -> paymentService.markBookingAsPaid(nonExistentId))
                    .isInstanceOf(BookingNotFoundException.class)
                    .hasMessageContaining("Booking not found");
        }

        @Test
        @DisplayName("Should update already completed booking status")
        void markBookingAsPaid_alreadyPaidBooking_stillUpdates() {
            // Pre-set to completed
            booking.setPaymentStatus(PaymentStatus.COMPLETED);
            bookingRepository.save(booking);

            // Act - should still work (idempotent operation)
            Booking result = paymentService.markBookingAsPaid(booking.getId());

            // Assert
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("Get Booking Payment Status Tests")
    class GetBookingPaymentStatusTests {

        @Test
        @DisplayName("Should return booking with pending payment status")
        void getBookingPaymentStatus_pendingPayment_returnsBooking() {
            Booking result = paymentService.getBookingPaymentStatus(booking.getId());

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(booking.getId());
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(result.getTotalPrice()).isEqualTo(75.0);
        }

        @Test
        @DisplayName("Should return booking with completed payment status")
        void getBookingPaymentStatus_completedPayment_returnsBooking() {
            booking.setPaymentStatus(PaymentStatus.COMPLETED);
            bookingRepository.save(booking);

            Booking result = paymentService.getBookingPaymentStatus(booking.getId());

            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should throw exception for non-existent booking")
        void getBookingPaymentStatus_nonExistentBooking_throwsException() {
            UUID nonExistentId = UUID.randomUUID();

            assertThatThrownBy(() -> paymentService.getBookingPaymentStatus(nonExistentId))
                    .isInstanceOf(BookingNotFoundException.class)
                    .hasMessageContaining("Booking not found");
        }
    }

    @Nested
    @DisplayName("Validate Booking For Payment Tests")
    class ValidateBookingForPaymentTests {

        @Test
        @DisplayName("Should validate booking with pending payment")
        void validateBookingForPayment_pendingPayment_returnsBooking() {
            Booking result = paymentService.validateBookingForPayment(booking.getId());

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(booking.getId());
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("Should throw exception for already paid booking")
        void validateBookingForPayment_alreadyPaid_throwsException() {
            booking.setPaymentStatus(PaymentStatus.COMPLETED);
            bookingRepository.save(booking);

            assertThatThrownBy(() -> paymentService.validateBookingForPayment(booking.getId()))
                    .isInstanceOf(PaymentAlreadyCompletedException.class)
                    .hasMessageContaining("already paid");
        }

        @Test
        @DisplayName("Should throw exception for non-existent booking")
        void validateBookingForPayment_nonExistentBooking_throwsException() {
            UUID nonExistentId = UUID.randomUUID();

            assertThatThrownBy(() -> paymentService.validateBookingForPayment(nonExistentId))
                    .isInstanceOf(BookingNotFoundException.class)
                    .hasMessageContaining("Booking not found");
        }

        @Test
        @DisplayName("Should allow validation for failed payment booking")
        void validateBookingForPayment_failedPayment_returnsBooking() {
            booking.setPaymentStatus(PaymentStatus.FAILED);
            bookingRepository.save(booking);

            Booking result = paymentService.validateBookingForPayment(booking.getId());

            assertThat(result).isNotNull();
            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("Update Payment Status Tests")
    class UpdatePaymentStatusTests {

        @Test
        @DisplayName("Should update payment status from PENDING to COMPLETED")
        void updatePaymentStatus_pendingToCompleted_updatesSuccessfully() {
            Booking result = paymentService.updatePaymentStatus(booking.getId(), PaymentStatus.COMPLETED);

            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);

            // Verify persistence
            Booking persistedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
            assertThat(persistedBooking.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should update payment status from PENDING to FAILED")
        void updatePaymentStatus_pendingToFailed_updatesSuccessfully() {
            Booking result = paymentService.updatePaymentStatus(booking.getId(), PaymentStatus.FAILED);

            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);

            // Verify persistence
            Booking persistedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
            assertThat(persistedBooking.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        }

        @Test
        @DisplayName("Should update payment status to REFUNDED")
        void updatePaymentStatus_toRefunded_updatesSuccessfully() {
            booking.setPaymentStatus(PaymentStatus.COMPLETED);
            bookingRepository.save(booking);

            Booking result = paymentService.updatePaymentStatus(booking.getId(), PaymentStatus.REFUNDED);

            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);

            // Verify persistence
            Booking persistedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
            assertThat(persistedBooking.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }

        @Test
        @DisplayName("Should throw exception for non-existent booking")
        void updatePaymentStatus_nonExistentBooking_throwsException() {
            UUID nonExistentId = UUID.randomUUID();

            assertThatThrownBy(() -> paymentService.updatePaymentStatus(nonExistentId, PaymentStatus.COMPLETED))
                    .isInstanceOf(BookingNotFoundException.class)
                    .hasMessageContaining("Booking not found");
        }
    }

    @Nested
    @DisplayName("Database Persistence Tests")
    class DatabasePersistenceTests {

        @Test
        @DisplayName("Should persist multiple status updates correctly")
        void multipleStatusUpdates_allPersisted() {
            // Update to FAILED
            paymentService.updatePaymentStatus(booking.getId(), PaymentStatus.FAILED);
            Booking afterFailed = bookingRepository.findById(booking.getId()).orElseThrow();
            assertThat(afterFailed.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);

            // Update to PENDING again
            paymentService.updatePaymentStatus(booking.getId(), PaymentStatus.PENDING);
            Booking afterPending = bookingRepository.findById(booking.getId()).orElseThrow();
            assertThat(afterPending.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);

            // Update to COMPLETED
            paymentService.updatePaymentStatus(booking.getId(), PaymentStatus.COMPLETED);
            Booking afterCompleted = bookingRepository.findById(booking.getId()).orElseThrow();
            assertThat(afterCompleted.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should handle concurrent-like operations on same booking")
        void concurrentOperations_maintainsConsistency() {
            // Simulate multiple operations
            paymentService.getBookingPaymentStatus(booking.getId());
            paymentService.validateBookingForPayment(booking.getId());
            paymentService.markBookingAsPaid(booking.getId());

            // Final state should be COMPLETED
            Booking finalState = bookingRepository.findById(booking.getId()).orElseThrow();
            assertThat(finalState.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should maintain booking relationships after payment update")
        void paymentUpdate_maintainsRelationships() {
            paymentService.markBookingAsPaid(booking.getId());

            Booking updatedBooking = bookingRepository.findById(booking.getId()).orElseThrow();

            // Verify relationships are maintained
            assertThat(updatedBooking.getTool()).isNotNull();
            assertThat(updatedBooking.getTool().getId()).isEqualTo(tool.getId());
            assertThat(updatedBooking.getRenter()).isNotNull();
            assertThat(updatedBooking.getRenter().getId()).isEqualTo(renter.getId());
            assertThat(updatedBooking.getOwner()).isNotNull();
            assertThat(updatedBooking.getOwner().getId()).isEqualTo(owner.getId());
        }
    }

    @Nested
    @DisplayName("Multiple Bookings Tests")
    class MultipleBookingsTests {

        @Test
        @DisplayName("Should handle payments for multiple bookings independently")
        void multipleBookings_independentPayments() {
            // Create second booking
            Booking booking2 = new Booking();
            booking2.setTool(tool);
            booking2.setRenter(renter);
            booking2.setOwner(owner);
            booking2.setStartDate(LocalDate.now().plusDays(5));
            booking2.setEndDate(LocalDate.now().plusDays(7));
            booking2.setStatus(BookingStatus.APPROVED);
            booking2.setTotalPrice(50.0);
            booking2.setPaymentStatus(PaymentStatus.PENDING);
            booking2 = bookingRepository.save(booking2);

            // Pay first booking
            paymentService.markBookingAsPaid(booking.getId());

            // Verify first booking is paid
            Booking first = bookingRepository.findById(booking.getId()).orElseThrow();
            assertThat(first.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);

            // Verify second booking is still pending
            Booking second = bookingRepository.findById(booking2.getId()).orElseThrow();
            assertThat(second.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        }

        @Test
        @DisplayName("Should get correct status for each booking")
        void multipleBookings_correctStatuses() {
            // Create bookings with different statuses
            Booking completedBooking = new Booking();
            completedBooking.setTool(tool);
            completedBooking.setRenter(renter);
            completedBooking.setOwner(owner);
            completedBooking.setStartDate(LocalDate.now().plusDays(10));
            completedBooking.setEndDate(LocalDate.now().plusDays(12));
            completedBooking.setStatus(BookingStatus.APPROVED);
            completedBooking.setTotalPrice(100.0);
            completedBooking.setPaymentStatus(PaymentStatus.COMPLETED);
            completedBooking = bookingRepository.save(completedBooking);

            Booking failedBooking = new Booking();
            failedBooking.setTool(tool);
            failedBooking.setRenter(renter);
            failedBooking.setOwner(owner);
            failedBooking.setStartDate(LocalDate.now().plusDays(15));
            failedBooking.setEndDate(LocalDate.now().plusDays(17));
            failedBooking.setStatus(BookingStatus.APPROVED);
            failedBooking.setTotalPrice(125.0);
            failedBooking.setPaymentStatus(PaymentStatus.FAILED);
            failedBooking = bookingRepository.save(failedBooking);

            // Verify each booking has correct status
            assertThat(paymentService.getBookingPaymentStatus(booking.getId()).getPaymentStatus())
                    .isEqualTo(PaymentStatus.PENDING);
            assertThat(paymentService.getBookingPaymentStatus(completedBooking.getId()).getPaymentStatus())
                    .isEqualTo(PaymentStatus.COMPLETED);
            assertThat(paymentService.getBookingPaymentStatus(failedBooking.getId()).getPaymentStatus())
                    .isEqualTo(PaymentStatus.FAILED);
        }
    }
}
