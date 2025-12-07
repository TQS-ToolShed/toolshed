package com.toolshed.backend.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import com.toolshed.backend.dto.CreateBookingRequest;
import com.toolshed.backend.dto.BookingResponse;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ToolRepository toolRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private Tool tool;
    private User renter;

    @BeforeEach
    void setUp() {
        tool = new Tool();
        tool.setId(UUID.randomUUID());
        tool.setPricePerDay(10.0);
        User owner = new User();
        owner.setId(UUID.randomUUID());
        tool.setOwner(owner);

        renter = new User();
        renter.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should create booking when dates are valid and not overlapping")
    void createBookingSuccess() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2);
        when(toolRepository.findById(tool.getId())).thenReturn(Optional.of(tool));
        when(userRepository.findById(renter.getId())).thenReturn(Optional.of(renter));
        when(bookingRepository.findOverlappingBookings(tool.getId(), start, end))
                .thenReturn(Collections.emptyList());

        Booking saved = new Booking();
        saved.setId(UUID.randomUUID());
        saved.setTool(tool);
        saved.setRenter(renter);
        saved.setOwner(tool.getOwner());
        saved.setStartDate(start);
        saved.setEndDate(end);
        saved.setStatus(BookingStatus.PENDING);
        saved.setPaymentStatus(PaymentStatus.PENDING);
        saved.setTotalPrice(30.0); // 3 days * 10

        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

        var response = bookingService.createBooking(CreateBookingRequest.builder()
                .toolId(tool.getId())
                .renterId(renter.getId())
                .startDate(start)
                .endDate(end)
                .build());

        assertThat(response.getId()).isEqualTo(saved.getId());
        assertThat(response.getTotalPrice()).isEqualTo(30.0);
        assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        Booking toPersist = bookingCaptor.getValue();
        assertThat(toPersist.getTotalPrice()).isEqualTo(30.0);
        assertThat(toPersist.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(toPersist.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("Should reject past start date")
    void createBookingPastDate() {
        LocalDate start = LocalDate.now().minusDays(1);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .toolId(tool.getId())
                .renterId(renter.getId())
                .startDate(start)
                .endDate(LocalDate.now().plusDays(1))
                .build();

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Dates cannot be in the past")
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should reject end date before start date")
    void createBookingEndBeforeStart() {
        LocalDate start = LocalDate.now().plusDays(3);
        LocalDate end = LocalDate.now().plusDays(1);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .toolId(tool.getId())
                .renterId(renter.getId())
                .startDate(start)
                .endDate(end)
                .build();

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("End date must be on or after start date");
    }

    @Test
    @DisplayName("Should reject overlaps")
    void createBookingOverlap() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(1);
        when(toolRepository.findById(tool.getId())).thenReturn(Optional.of(tool));
        when(userRepository.findById(renter.getId())).thenReturn(Optional.of(renter));
        when(bookingRepository.findOverlappingBookings(tool.getId(), start, end))
                .thenReturn(java.util.List.of(new Booking()));

        CreateBookingRequest request = CreateBookingRequest.builder()
                .toolId(tool.getId())
                .renterId(renter.getId())
                .startDate(start)
                .endDate(end)
                .build();

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Should list bookings for tool")
    void getBookingsForTool() {
        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setTool(tool);
        booking.setRenter(renter);
        booking.setOwner(tool.getOwner());
        booking.setStartDate(LocalDate.now());
        booking.setEndDate(LocalDate.now().plusDays(1));
        booking.setStatus(BookingStatus.PENDING);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setTotalPrice(10.0);

        when(bookingRepository.findByToolId(tool.getId())).thenReturn(List.of(booking));

        var result = bookingService.getBookingsForTool(tool.getId());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(booking.getId());
        assertThat(result.getFirst().getToolId()).isEqualTo(tool.getId());
    }

    @Test
    @DisplayName("Should update booking status from PENDING to APPROVED")
    void updateBookingStatusFromPending() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setTool(tool);
        tool.setActive(true);
        booking.setRenter(renter);
        booking.setOwner(tool.getOwner());
        booking.setStatus(BookingStatus.PENDING);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setStartDate(LocalDate.now());
        booking.setEndDate(LocalDate.now().plusDays(1));

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(toolRepository.save(any(Tool.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = bookingService.updateBookingStatus(bookingId, BookingStatus.APPROVED);

        assertThat(response.getStatus()).isEqualTo(BookingStatus.APPROVED);
        verify(bookingRepository).save(any(Booking.class));
        verify(toolRepository).save(tool);
        assertThat(tool.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should keep tool available if approval is outside booking window")
    void updateBookingStatusFutureWindowKeepsToolActive() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setTool(tool);
        tool.setActive(true);
        booking.setRenter(renter);
        booking.setOwner(tool.getOwner());
        booking.setStatus(BookingStatus.PENDING);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setStartDate(LocalDate.now().plusDays(5));
        booking.setEndDate(LocalDate.now().plusDays(7));

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(toolRepository.save(any(Tool.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = bookingService.updateBookingStatus(bookingId, BookingStatus.APPROVED);

        assertThat(response.getStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThat(tool.isActive()).isTrue();
        verify(toolRepository).save(tool);
    }

    @Test
    @DisplayName("Should not approve if another booking is already approved in the same window")
    void updateBookingStatusConflictingApproved() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setTool(tool);
        tool.setActive(true);
        booking.setRenter(renter);
        booking.setOwner(tool.getOwner());
        booking.setStatus(BookingStatus.PENDING);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setStartDate(LocalDate.now());
        booking.setEndDate(LocalDate.now().plusDays(2));

        Booking existingApproved = new Booking();
        existingApproved.setId(UUID.randomUUID());
        existingApproved.setTool(tool);
        existingApproved.setStatus(BookingStatus.APPROVED);
        existingApproved.setStartDate(LocalDate.now());
        existingApproved.setEndDate(LocalDate.now().plusDays(1));

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.findOverlappingBookings(eq(tool.getId()), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(booking, existingApproved));

        assertThatThrownBy(() -> bookingService.updateBookingStatus(bookingId, BookingStatus.APPROVED))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Should not allow status change once approved or rejected")
    void updateBookingStatusAlreadyFinal() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setTool(tool);
        booking.setRenter(renter);
        booking.setOwner(tool.getOwner());
        booking.setStatus(BookingStatus.APPROVED);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.updateBookingStatus(bookingId, BookingStatus.REJECTED))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should reject unsupported status transitions")
    void updateBookingStatusUnsupported() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setTool(tool);
        booking.setRenter(renter);
        booking.setOwner(tool.getOwner());
        booking.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.updateBookingStatus(bookingId, BookingStatus.CANCELLED))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should map bookings for owner with tool and renter details")
    void getBookingsForOwnerMapsFields() {
        UUID ownerId = tool.getOwner().getId();
        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setTool(tool);
        tool.setTitle("Cordless Drill");
        booking.setRenter(renter);
        renter.setFirstName("Ada");
        renter.setLastName("Lovelace");
        booking.setOwner(tool.getOwner());
        booking.setStartDate(LocalDate.of(2024, 1, 10));
        booking.setEndDate(LocalDate.of(2024, 1, 12));
        booking.setStatus(BookingStatus.APPROVED);
        booking.setTotalPrice(42.0);

        when(bookingRepository.findByOwnerId(ownerId)).thenReturn(List.of(booking));

        List<com.toolshed.backend.dto.OwnerBookingResponse> responses = bookingService.getBookingsForOwner(ownerId);

        assertThat(responses).hasSize(1);
        com.toolshed.backend.dto.OwnerBookingResponse response = responses.getFirst();
        assertThat(response.getId()).isEqualTo(booking.getId());
        assertThat(response.getToolId()).isEqualTo(tool.getId());
        assertThat(response.getToolTitle()).isEqualTo("Cordless Drill");
        assertThat(response.getRenterId()).isEqualTo(renter.getId());
        assertThat(response.getRenterName()).isEqualTo("Ada Lovelace");
        assertThat(response.getStartDate()).isEqualTo(booking.getStartDate());
        assertThat(response.getEndDate()).isEqualTo(booking.getEndDate());
        assertThat(response.getStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThat(response.getTotalPrice()).isEqualTo(42.0);
    }

    @Test
    @DisplayName("Should list bookings for renter and map fields")
    void getBookingsForRenter() {
        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setTool(tool);
        booking.setRenter(renter);
        booking.setOwner(tool.getOwner());
        booking.setStartDate(LocalDate.now());
        booking.setEndDate(LocalDate.now().plusDays(1));
        booking.setStatus(BookingStatus.PENDING);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setTotalPrice(10.0);
        tool.setTitle("Impact Driver");

        when(bookingRepository.findByRenterId(renter.getId())).thenReturn(List.of(booking));

        List<BookingResponse> responses = bookingService.getBookingsForRenter(renter.getId());

        assertThat(responses).hasSize(1);
        BookingResponse response = responses.getFirst();
        assertThat(response.getRenterId()).isEqualTo(renter.getId());
        assertThat(response.getToolId()).isEqualTo(tool.getId());
        assertThat(response.getOwnerId()).isEqualTo(tool.getOwner().getId());
        assertThat(response.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(response.getToolTitle()).isEqualTo("Impact Driver");
        assertThat(response.getTotalPrice()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("Should list bookings for a tool and include tool title")
    void getBookingsForTool() {
        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setTool(tool);
        booking.setRenter(renter);
        booking.setOwner(tool.getOwner());
        booking.setStartDate(LocalDate.now().plusDays(2));
        booking.setEndDate(LocalDate.now().plusDays(3));
        booking.setStatus(BookingStatus.APPROVED);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        tool.setTitle("Power Saw");

        when(bookingRepository.findByToolId(tool.getId())).thenReturn(List.of(booking));

        List<BookingResponse> responses = bookingService.getBookingsForTool(tool.getId());

        assertThat(responses).hasSize(1);
        BookingResponse response = responses.getFirst();
        assertThat(response.getToolTitle()).isEqualTo("Power Saw");
        assertThat(response.getToolId()).isEqualTo(tool.getId());
        assertThat(response.getStatus()).isEqualTo(BookingStatus.APPROVED);
    }
}
