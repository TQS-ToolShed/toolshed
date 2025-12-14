package com.toolshed.backend.repository;

import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.PaymentStatus;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BookingRepositoryEarningsTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ToolRepository toolRepository;

    private User owner;
    private User renter;
    private Tool tool;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        toolRepository.deleteAll();
        userRepository.deleteAll();

        // Create owner
        owner = User.builder()
                .firstName("John")
                .lastName("Owner")
                .email("owner@test.com")
                .password("hashedpass")
                .role(UserRole.SUPPLIER)
                .status(UserStatus.ACTIVE)
                .reputationScore(5.0)
                .walletBalance(0.0)
                .registeredDate(LocalDateTime.now())
                .build();
        owner = userRepository.save(owner);

        // Create renter
        renter = User.builder()
                .firstName("Jane")
                .lastName("Renter")
                .email("renter@test.com")
                .password("hashedpass")
                .role(UserRole.RENTER)
                .status(UserStatus.ACTIVE)
                .reputationScore(4.5)
                .walletBalance(100.0)
                .registeredDate(LocalDateTime.now())
                .build();
        renter = userRepository.save(renter);

        // Create tool
        tool = Tool.builder()
                .title("Power Drill")
                .description("18V cordless drill")
                .pricePerDay(10.0)
                .district("Lisboa")
                .owner(owner)
                .active(true)
                .overallRating(0.0)
                .numRatings(0)
                .build();
        tool = toolRepository.save(tool);
    }

    @Test
    @DisplayName("Should return completed bookings grouped by month for owner")
    void findCompletedBookingsByOwnerIdGroupedByMonth() {
        // Create bookings for different months
        // January 2024
        Booking jan2024Booking = Booking.builder()
                .tool(tool)
                .renter(renter)
                .owner(owner)
                .startDate(LocalDate.of(2024, 1, 10))
                .endDate(LocalDate.of(2024, 1, 12))
                .status(BookingStatus.COMPLETED)
                .paymentStatus(PaymentStatus.COMPLETED)
                .totalPrice(30.0)
                .build();
        bookingRepository.save(jan2024Booking);

        // February 2024
        Booking feb2024Booking1 = Booking.builder()
                .tool(tool)
                .renter(renter)
                .owner(owner)
                .startDate(LocalDate.of(2024, 2, 5))
                .endDate(LocalDate.of(2024, 2, 7))
                .status(BookingStatus.COMPLETED)
                .paymentStatus(PaymentStatus.COMPLETED)
                .totalPrice(30.0)
                .build();
        bookingRepository.save(feb2024Booking1);

        Booking feb2024Booking2 = Booking.builder()
                .tool(tool)
                .renter(renter)
                .owner(owner)
                .startDate(LocalDate.of(2024, 2, 15))
                .endDate(LocalDate.of(2024, 2, 17))
                .status(BookingStatus.COMPLETED)
                .paymentStatus(PaymentStatus.COMPLETED)
                .totalPrice(30.0)
                .build();
        bookingRepository.save(feb2024Booking2);

        // Pending booking (should not be included)
        Booking pendingBooking = Booking.builder()
                .tool(tool)
                .renter(renter)
                .owner(owner)
                .startDate(LocalDate.of(2024, 3, 1))
                .endDate(LocalDate.of(2024, 3, 3))
                .status(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .totalPrice(30.0)
                .build();
        bookingRepository.save(pendingBooking);

        // Test: Get completed bookings for owner
        List<Booking> completedBookings = bookingRepository.findCompletedBookingsByOwnerId(owner.getId());

        assertThat(completedBookings).hasSize(3);
        assertThat(completedBookings)
                .extracting(Booking::getStatus)
                .containsOnly(BookingStatus.COMPLETED);
        assertThat(completedBookings)
                .extracting(Booking::getTotalPrice)
                .containsExactlyInAnyOrder(30.0, 30.0, 30.0);
    }

    @Test
    @DisplayName("Should return empty list when owner has no completed bookings")
    void findCompletedBookingsByOwnerIdWhenNoBookings() {
        List<Booking> completedBookings = bookingRepository.findCompletedBookingsByOwnerId(owner.getId());
        
        assertThat(completedBookings).isEmpty();
    }

    @Test
    @DisplayName("Should only return bookings for specific owner")
    void findCompletedBookingsByOwnerIdForSpecificOwnerOnly() {
        // Create another owner
        User anotherOwner = User.builder()
                .firstName("Another")
                .lastName("Owner")
                .email("another@test.com")
                .password("hashedpass")
                .role(UserRole.SUPPLIER)
                .status(UserStatus.ACTIVE)
                .reputationScore(5.0)
                .walletBalance(0.0)
                .registeredDate(LocalDateTime.now())
                .build();
        anotherOwner = userRepository.save(anotherOwner);

        Tool anotherTool = Tool.builder()
                .title("Hammer")
                .description("Heavy duty hammer")
                .pricePerDay(5.0)
                .district("Porto")
                .owner(anotherOwner)
                .active(true)
                .overallRating(0.0)
                .numRatings(0)
                .build();
        anotherTool = toolRepository.save(anotherTool);

        // Booking for first owner
        Booking ownerBooking = Booking.builder()
                .tool(tool)
                .renter(renter)
                .owner(owner)
                .startDate(LocalDate.of(2024, 1, 10))
                .endDate(LocalDate.of(2024, 1, 12))
                .status(BookingStatus.COMPLETED)
                .paymentStatus(PaymentStatus.COMPLETED)
                .totalPrice(30.0)
                .build();
        bookingRepository.save(ownerBooking);

        // Booking for another owner
        Booking anotherOwnerBooking = Booking.builder()
                .tool(anotherTool)
                .renter(renter)
                .owner(anotherOwner)
                .startDate(LocalDate.of(2024, 1, 15))
                .endDate(LocalDate.of(2024, 1, 17))
                .status(BookingStatus.COMPLETED)
                .paymentStatus(PaymentStatus.COMPLETED)
                .totalPrice(15.0)
                .build();
        bookingRepository.save(anotherOwnerBooking);

        // Test: Get completed bookings for first owner only
        List<Booking> completedBookings = bookingRepository.findCompletedBookingsByOwnerId(owner.getId());

        assertThat(completedBookings).hasSize(1);
        assertThat(completedBookings.get(0).getOwner().getId()).isEqualTo(owner.getId());
        assertThat(completedBookings.get(0).getTotalPrice()).isEqualTo(30.0);
    }
}
