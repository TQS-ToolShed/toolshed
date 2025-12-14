package com.toolshed.backend.service;

import com.toolshed.backend.dto.MonthlyEarnings;
import com.toolshed.backend.dto.OwnerEarningsResponse;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.PaymentStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceEarningsTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private UUID ownerId;
    private User owner;
    private User renter;
    private Tool tool;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        
        owner = User.builder()
                .id(ownerId)
                .firstName("John")
                .lastName("Owner")
                .email("owner@test.com")
                .build();

        renter = User.builder()
                .id(UUID.randomUUID())
                .firstName("Jane")
                .lastName("Renter")
                .email("renter@test.com")
                .build();

        tool = Tool.builder()
                .id(UUID.randomUUID())
                .title("Power Drill")
                .pricePerDay(10.0)
                .owner(owner)
                .build();
    }

    @Test
    @DisplayName("Should calculate earnings grouped by month")
    void getOwnerEarnings_shouldGroupByMonth() {
        // Arrange: Create bookings for different months
        List<Booking> bookings = new ArrayList<>();

        // January 2024 - 2 bookings
        bookings.add(createBooking(LocalDate.of(2024, 1, 10), LocalDate.of(2024, 1, 12), 30.0));
        bookings.add(createBooking(LocalDate.of(2024, 1, 20), LocalDate.of(2024, 1, 22), 30.0));

        // February 2024 - 1 booking
        bookings.add(createBooking(LocalDate.of(2024, 2, 5), LocalDate.of(2024, 2, 7), 30.0));

        when(bookingRepository.findCompletedBookingsByOwnerId(ownerId)).thenReturn(bookings);

        // Act
        OwnerEarningsResponse response = bookingService.getOwnerEarnings(ownerId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTotalEarnings()).isEqualTo(90.0);
        assertThat(response.getMonthlyEarnings()).hasSize(2);

        // Check January 2024
        MonthlyEarnings jan2024 = response.getMonthlyEarnings().stream()
                .filter(m -> m.getYear() == 2024 && m.getMonth() == 1)
                .findFirst()
                .orElse(null);
        assertThat(jan2024).isNotNull();
        assertThat(jan2024.getTotalEarnings()).isEqualTo(60.0);
        assertThat(jan2024.getBookingCount()).isEqualTo(2L);

        // Check February 2024
        MonthlyEarnings feb2024 = response.getMonthlyEarnings().stream()
                .filter(m -> m.getYear() == 2024 && m.getMonth() == 2)
                .findFirst()
                .orElse(null);
        assertThat(feb2024).isNotNull();
        assertThat(feb2024.getTotalEarnings()).isEqualTo(30.0);
        assertThat(feb2024.getBookingCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should return empty earnings when owner has no completed bookings")
    void getOwnerEarnings_shouldReturnEmptyWhenNoBookings() {
        // Arrange
        when(bookingRepository.findCompletedBookingsByOwnerId(ownerId)).thenReturn(new ArrayList<>());

        // Act
        OwnerEarningsResponse response = bookingService.getOwnerEarnings(ownerId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTotalEarnings()).isEqualTo(0.0);
        assertThat(response.getMonthlyEarnings()).isEmpty();
    }

    @Test
    @DisplayName("Should sort monthly earnings by year and month descending")
    void getOwnerEarnings_shouldSortByYearMonthDescending() {
        // Arrange: Create bookings spanning multiple years
        List<Booking> bookings = new ArrayList<>();
        
        bookings.add(createBooking(LocalDate.of(2023, 12, 1), LocalDate.of(2023, 12, 3), 30.0));
        bookings.add(createBooking(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 3), 30.0));
        bookings.add(createBooking(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 3), 30.0));

        when(bookingRepository.findCompletedBookingsByOwnerId(ownerId)).thenReturn(bookings);

        // Act
        OwnerEarningsResponse response = bookingService.getOwnerEarnings(ownerId);

        // Assert
        assertThat(response.getMonthlyEarnings()).hasSize(3);
        
        // Verify ordering (most recent first)
        List<MonthlyEarnings> earnings = response.getMonthlyEarnings();
        assertThat(earnings.get(0).getYear()).isEqualTo(2024);
        assertThat(earnings.get(0).getMonth()).isEqualTo(2);
        
        assertThat(earnings.get(1).getYear()).isEqualTo(2024);
        assertThat(earnings.get(1).getMonth()).isEqualTo(1);
        
        assertThat(earnings.get(2).getYear()).isEqualTo(2023);
        assertThat(earnings.get(2).getMonth()).isEqualTo(12);
    }

    @Test
    @DisplayName("Should handle bookings with null totalPrice gracefully")
    void getOwnerEarnings_shouldHandleNullTotalPrice() {
        // Arrange
        List<Booking> bookings = new ArrayList<>();
        
        Booking bookingWithPrice = createBooking(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 3), 30.0);
        Booking bookingWithoutPrice = createBooking(LocalDate.of(2024, 1, 10), LocalDate.of(2024, 1, 12), null);
        
        bookings.add(bookingWithPrice);
        bookings.add(bookingWithoutPrice);

        when(bookingRepository.findCompletedBookingsByOwnerId(ownerId)).thenReturn(bookings);

        // Act
        OwnerEarningsResponse response = bookingService.getOwnerEarnings(ownerId);

        // Assert
        assertThat(response.getTotalEarnings()).isEqualTo(30.0);
        assertThat(response.getMonthlyEarnings()).hasSize(1);
        assertThat(response.getMonthlyEarnings().get(0).getTotalEarnings()).isEqualTo(30.0);
        assertThat(response.getMonthlyEarnings().get(0).getBookingCount()).isEqualTo(1L);
    }

    private Booking createBooking(LocalDate startDate, LocalDate endDate, Double totalPrice) {
        return Booking.builder()
                .id(UUID.randomUUID())
                .tool(tool)
                .renter(renter)
                .owner(owner)
                .startDate(startDate)
                .endDate(endDate)
                .status(BookingStatus.COMPLETED)
                .paymentStatus(PaymentStatus.COMPLETED)
                .totalPrice(totalPrice)
                .build();
    }
}
