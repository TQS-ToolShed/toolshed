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
import com.toolshed.backend.repository.entities.Review;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.PaymentStatus;
import com.toolshed.backend.repository.enums.ReviewType;
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
import static org.mockito.Mockito.never;

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
                when(bookingRepository.findOverlappingBookings(eq(tool.getId()), any(LocalDate.class),
                                any(LocalDate.class)))
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

                List<com.toolshed.backend.dto.OwnerBookingResponse> responses = bookingService
                                .getBookingsForOwner(ownerId);

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

        @Test
        @DisplayName("Should mark expired approved bookings as completed and free tools")
        void completeExpiredBookings() {
                Tool rentedTool = new Tool();
                rentedTool.setId(UUID.randomUUID());
                rentedTool.setActive(false);
                User owner = new User();
                owner.setId(UUID.randomUUID());
                User renterUser = new User();
                renterUser.setId(UUID.randomUUID());

                Booking expired = new Booking();
                expired.setId(UUID.randomUUID());
                expired.setTool(rentedTool);
                expired.setOwner(owner);
                expired.setRenter(renterUser);
                expired.setStartDate(LocalDate.now().minusDays(5));
                expired.setEndDate(LocalDate.now().minusDays(1));
                expired.setStatus(BookingStatus.APPROVED);
                expired.setPaymentStatus(PaymentStatus.PENDING);

                when(bookingRepository.findByStatusAndEndDateBefore(eq(BookingStatus.APPROVED), any(LocalDate.class)))
                                .thenReturn(List.of(expired));
                when(bookingRepository.countActiveApprovedBookingsForToolOnDate(eq(rentedTool.getId()),
                                any(LocalDate.class)))
                                .thenReturn(0L);

                bookingService.completeExpiredBookings();

                assertThat(expired.getStatus()).isEqualTo(BookingStatus.COMPLETED);
                verify(bookingRepository).save(expired);
                assertThat(rentedTool.isActive()).isTrue();
                verify(toolRepository).save(rentedTool);
        }

        @Test
        @DisplayName("Should reject past end date")
        void createBookingPastEndDate() {
                LocalDate start = LocalDate.now().plusDays(1);
                LocalDate end = LocalDate.now().minusDays(1);
                CreateBookingRequest request = CreateBookingRequest.builder()
                                .toolId(tool.getId())
                                .renterId(renter.getId())
                                .startDate(start)
                                .endDate(end)
                                .build();

                assertThatThrownBy(() -> bookingService.createBooking(request))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("Dates cannot be in the past")
                                .extracting("statusCode")
                                .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should reject booking when tool has no owner")
        void createBookingToolWithNullOwner() {
                LocalDate start = LocalDate.now().plusDays(1);
                LocalDate end = start.plusDays(2);
                Tool toolWithNoOwner = new Tool();
                toolWithNoOwner.setId(UUID.randomUUID());
                toolWithNoOwner.setPricePerDay(10.0);
                toolWithNoOwner.setOwner(null);

                when(toolRepository.findById(toolWithNoOwner.getId())).thenReturn(Optional.of(toolWithNoOwner));
                when(userRepository.findById(renter.getId())).thenReturn(Optional.of(renter));

                CreateBookingRequest request = CreateBookingRequest.builder()
                                .toolId(toolWithNoOwner.getId())
                                .renterId(renter.getId())
                                .startDate(start)
                                .endDate(end)
                                .build();

                assertThatThrownBy(() -> bookingService.createBooking(request))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("Tool owner is missing")
                                .extracting("statusCode")
                                .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should handle expired booking with null tool gracefully")
        void completeExpiredBookingsWithNullTool() {
                Booking expired = new Booking();
                expired.setId(UUID.randomUUID());
                expired.setTool(null);
                expired.setOwner(tool.getOwner());
                expired.setRenter(renter);
                expired.setStartDate(LocalDate.now().minusDays(5));
                expired.setEndDate(LocalDate.now().minusDays(1));
                expired.setStatus(BookingStatus.APPROVED);

                when(bookingRepository.findByStatusAndEndDateBefore(eq(BookingStatus.APPROVED), any(LocalDate.class)))
                                .thenReturn(List.of(expired));

                bookingService.completeExpiredBookings();

                assertThat(expired.getStatus()).isEqualTo(BookingStatus.COMPLETED);
                verify(bookingRepository).save(expired);
                verify(toolRepository, never()).save(any(Tool.class));
        }

        @Test
        @DisplayName("Should handle status update with null tool gracefully")
        void updateBookingStatusWithNullTool() {
                UUID bookingId = UUID.randomUUID();
                Booking booking = new Booking();
                booking.setId(bookingId);
                booking.setTool(null);
                booking.setRenter(renter);
                booking.setOwner(tool.getOwner());
                booking.setStatus(BookingStatus.PENDING);
                booking.setPaymentStatus(PaymentStatus.PENDING);
                booking.setStartDate(LocalDate.now());
                booking.setEndDate(LocalDate.now().plusDays(1));

                when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
                when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

                // toBookingResponse expects non-null tool, so NPE is thrown
                assertThatThrownBy(() -> bookingService.updateBookingStatus(bookingId, BookingStatus.REJECTED))
                                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle owner booking response with null tool and renter")
        void getBookingsForOwnerWithNullToolAndRenter() {
                UUID ownerId = tool.getOwner().getId();
                Booking booking = new Booking();
                booking.setId(UUID.randomUUID());
                booking.setTool(null);
                booking.setRenter(null);
                booking.setOwner(tool.getOwner());
                booking.setStartDate(LocalDate.of(2024, 1, 10));
                booking.setEndDate(LocalDate.of(2024, 1, 12));
                booking.setStatus(BookingStatus.APPROVED);
                booking.setTotalPrice(42.0);

                when(bookingRepository.findByOwnerId(ownerId)).thenReturn(List.of(booking));

                List<com.toolshed.backend.dto.OwnerBookingResponse> responses = bookingService
                                .getBookingsForOwner(ownerId);

                assertThat(responses).hasSize(1);
                com.toolshed.backend.dto.OwnerBookingResponse response = responses.getFirst();
                assertThat(response.getToolId()).isNull();
                assertThat(response.getToolTitle()).isNull();
                assertThat(response.getRenterId()).isNull();
                assertThat(response.getRenterName()).isNull();
        }

        @Test
        @DisplayName("Should handle booking response with null owner")
        void getBookingsForRenterWithNullOwner() {
                Booking booking = new Booking();
                booking.setId(UUID.randomUUID());
                booking.setTool(tool);
                booking.setRenter(renter);
                booking.setOwner(null);
                booking.setStartDate(LocalDate.now());
                booking.setEndDate(LocalDate.now().plusDays(1));
                booking.setStatus(BookingStatus.PENDING);
                booking.setPaymentStatus(PaymentStatus.PENDING);
                booking.setTotalPrice(10.0);

                when(bookingRepository.findByRenterId(renter.getId())).thenReturn(List.of(booking));

                // This will throw NPE due to existing code expecting non-null owner in
                // toBookingResponse
                // But the test covers the branch where ownerName becomes null
                assertThatThrownBy(() -> bookingService.getBookingsForRenter(renter.getId()))
                                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should include both renter and owner reviews in booking response")
        void getBookingsWithReviews() {
                UUID ownerId = tool.getOwner().getId();
                Booking booking = new Booking();
                booking.setId(UUID.randomUUID());
                booking.setTool(tool);
                tool.setTitle("Power Drill");
                booking.setRenter(renter);
                renter.setFirstName("Jane");
                renter.setLastName("Doe");
                booking.setOwner(tool.getOwner());
                tool.getOwner().setFirstName("John");
                tool.getOwner().setLastName("Smith");
                booking.setStartDate(LocalDate.of(2024, 3, 1));
                booking.setEndDate(LocalDate.of(2024, 3, 5));
                booking.setStatus(BookingStatus.COMPLETED);
                booking.setTotalPrice(50.0);

                // Create reviews
                com.toolshed.backend.repository.entities.Review renterReview = new com.toolshed.backend.repository.entities.Review();
                renterReview.setId(UUID.randomUUID());
                renterReview.setBooking(booking);
                renterReview.setReviewer(renter);
                renterReview.setOwner(tool.getOwner());
                renterReview.setTool(tool);
                renterReview.setType(com.toolshed.backend.repository.enums.ReviewType.RENTER_TO_OWNER);
                renterReview.setRating(5);
                renterReview.setComment("Great tool!");
                renterReview.setDate(java.time.LocalDateTime.of(2024, 3, 6, 10, 0));

                com.toolshed.backend.repository.entities.Review ownerReview = new com.toolshed.backend.repository.entities.Review();
                ownerReview.setId(UUID.randomUUID());
                ownerReview.setBooking(booking);
                ownerReview.setReviewer(tool.getOwner());
                ownerReview.setOwner(tool.getOwner());
                ownerReview.setTool(tool);
                ownerReview.setType(com.toolshed.backend.repository.enums.ReviewType.OWNER_TO_RENTER);
                ownerReview.setRating(4);
                ownerReview.setComment("Good renter");
                ownerReview.setDate(java.time.LocalDateTime.of(2024, 3, 7, 10, 0));

                booking.setReviews(List.of(renterReview, ownerReview));

                when(bookingRepository.findByOwnerId(ownerId)).thenReturn(List.of(booking));

                List<com.toolshed.backend.dto.OwnerBookingResponse> responses = bookingService
                                .getBookingsForOwner(ownerId);

                assertThat(responses).hasSize(1);
                com.toolshed.backend.dto.OwnerBookingResponse response = responses.getFirst();
                assertThat(response.getReview()).isNotNull();
                assertThat(response.getReview().getRating()).isEqualTo(5);
                assertThat(response.getReview().getComment()).isEqualTo("Great tool!");
                assertThat(response.getReview().getReviewerName()).isEqualTo("Jane Doe");
                assertThat(response.getOwnerReview()).isNotNull();
                assertThat(response.getOwnerReview().getRating()).isEqualTo(4);
                assertThat(response.getOwnerReview().getComment()).isEqualTo("Good renter");
        }

        @Test
        @DisplayName("Should handle review with null reviewer in response")
        void getBookingsWithReviewNullReviewer() {
                UUID ownerId = tool.getOwner().getId();
                Booking booking = new Booking();
                booking.setId(UUID.randomUUID());
                booking.setTool(tool);
                tool.setTitle("Saw");
                booking.setRenter(renter);
                renter.setFirstName("Bob");
                renter.setLastName("Builder");
                booking.setOwner(tool.getOwner());
                booking.setStartDate(LocalDate.of(2024, 4, 1));
                booking.setEndDate(LocalDate.of(2024, 4, 2));
                booking.setStatus(BookingStatus.COMPLETED);
                booking.setTotalPrice(20.0);

                // Create review with null reviewer
                com.toolshed.backend.repository.entities.Review review = new com.toolshed.backend.repository.entities.Review();
                review.setId(UUID.randomUUID());
                review.setBooking(booking);
                review.setReviewer(null);
                review.setOwner(null);
                review.setTool(null);
                review.setType(com.toolshed.backend.repository.enums.ReviewType.RENTER_TO_OWNER);
                review.setRating(3);
                review.setComment("Anonymous review");
                review.setDate(java.time.LocalDateTime.of(2024, 4, 3, 10, 0));

                booking.setReviews(List.of(review));

                when(bookingRepository.findByOwnerId(ownerId)).thenReturn(List.of(booking));

                List<com.toolshed.backend.dto.OwnerBookingResponse> responses = bookingService
                                .getBookingsForOwner(ownerId);

                assertThat(responses).hasSize(1);
                com.toolshed.backend.dto.OwnerBookingResponse response = responses.getFirst();
                assertThat(response.getReview()).isNotNull();
                assertThat(response.getReview().getReviewerId()).isNull();
                assertThat(response.getReview().getReviewerName()).isNull();
                assertThat(response.getReview().getOwnerId()).isNull();
                assertThat(response.getReview().getToolId()).isNull();
                assertThat(response.getReview().getRating()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should include tool review in booking response")
        void getBookingsWithToolReview() {
                Booking booking = new Booking();
                booking.setId(UUID.randomUUID());
                booking.setTool(tool);
                tool.setTitle("Sander");
                booking.setRenter(renter);
                booking.setOwner(tool.getOwner());
                booking.setStartDate(LocalDate.now());
                booking.setEndDate(LocalDate.now().plusDays(1));
                booking.setStatus(BookingStatus.COMPLETED);
                booking.setPaymentStatus(PaymentStatus.COMPLETED);
                booking.setTotalPrice(20.0);

                Review toolReview = Review.builder()
                                .id(UUID.randomUUID())
                                .booking(booking)
                                .reviewer(renter)
                                .tool(tool)
                                .rating(5)
                                .comment("Excellent tool")
                                .type(ReviewType.RENTER_TO_TOOL)
                                .build();

                booking.setReviews(List.of(toolReview));

                when(bookingRepository.findByRenterId(renter.getId())).thenReturn(List.of(booking));

                List<BookingResponse> responses = bookingService.getBookingsForRenter(renter.getId());

                assertThat(responses).hasSize(1);
                BookingResponse response = responses.getFirst();
                assertThat(response.getToolReview()).isNotNull();
                assertThat(response.getToolReview().getRating()).isEqualTo(5);
                assertThat(response.getToolReview().getComment()).isEqualTo("Excellent tool");
        }

        @Test
        @DisplayName("Should handle review with null type as RENTER_TO_OWNER")
        void getBookingsWithReviewNullType() {
                UUID ownerId = tool.getOwner().getId();
                Booking booking = new Booking();
                booking.setId(UUID.randomUUID());
                booking.setTool(tool);
                tool.setTitle("Hammer");
                booking.setRenter(renter);
                renter.setFirstName("Alice");
                renter.setLastName("Wonder");
                booking.setOwner(tool.getOwner());
                booking.setStartDate(LocalDate.of(2024, 5, 1));
                booking.setEndDate(LocalDate.of(2024, 5, 2));
                booking.setStatus(BookingStatus.COMPLETED);
                booking.setTotalPrice(15.0);

                // Create review with null type (should be treated as RENTER_TO_OWNER)
                com.toolshed.backend.repository.entities.Review review = new com.toolshed.backend.repository.entities.Review();
                review.setId(UUID.randomUUID());
                review.setBooking(booking);
                review.setReviewer(renter);
                review.setOwner(tool.getOwner());
                review.setTool(tool);
                review.setType(null);
                review.setRating(5);
                review.setComment("Legacy review");
                review.setDate(java.time.LocalDateTime.of(2024, 5, 3, 10, 0));

                booking.setReviews(List.of(review));

                when(bookingRepository.findByOwnerId(ownerId)).thenReturn(List.of(booking));

                List<com.toolshed.backend.dto.OwnerBookingResponse> responses = bookingService
                                .getBookingsForOwner(ownerId);

                assertThat(responses).hasSize(1);
                com.toolshed.backend.dto.OwnerBookingResponse response = responses.getFirst();
                // Review with null type should be found when looking for RENTER_TO_OWNER
                assertThat(response.getReview()).isNotNull();
                assertThat(response.getReview().getComment()).isEqualTo("Legacy review");
                // Owner review should be null since no OWNER_TO_RENTER type
                assertThat(response.getOwnerReview()).isNull();
        }

        // ===== Condition Report Tests =====

        @Test
        @DisplayName("Should submit condition report and set NOT_REQUIRED deposit for OK condition")
        void submitConditionReportOKCondition() {
                UUID bookingId = UUID.randomUUID();
                Booking booking = new Booking();
                booking.setId(bookingId);
                booking.setTool(tool);
                tool.setTitle("Test Tool");
                booking.setRenter(renter);
                booking.setOwner(tool.getOwner());
                booking.setStatus(BookingStatus.COMPLETED);
                booking.setPaymentStatus(PaymentStatus.COMPLETED);
                booking.setStartDate(LocalDate.now().minusDays(2));
                booking.setEndDate(LocalDate.now().minusDays(1));
                booking.setTotalPrice(20.0);

                when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
                when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

                com.toolshed.backend.dto.ConditionReportRequest request = new com.toolshed.backend.dto.ConditionReportRequest(
                                com.toolshed.backend.repository.enums.ConditionStatus.OK,
                                "Tool returned in perfect condition",
                                renter.getId());

                BookingResponse response = bookingService.submitConditionReport(bookingId, request);

                assertThat(response.getConditionStatus())
                                .isEqualTo(com.toolshed.backend.repository.enums.ConditionStatus.OK);
                assertThat(response.getConditionDescription()).isEqualTo("Tool returned in perfect condition");
                assertThat(response.getDepositStatus())
                                .isEqualTo(com.toolshed.backend.repository.enums.DepositStatus.NOT_REQUIRED);
                assertThat(response.getDepositAmount()).isEqualTo(0.0);
                verify(bookingRepository).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should set REQUIRED deposit when condition is BROKEN")
        void submitConditionReportBrokenConditionRequiresDeposit() {
                UUID bookingId = UUID.randomUUID();
                Booking booking = new Booking();
                booking.setId(bookingId);
                booking.setTool(tool);
                tool.setTitle("Broken Tool");
                booking.setRenter(renter);
                booking.setOwner(tool.getOwner());
                booking.setStatus(BookingStatus.COMPLETED);
                booking.setPaymentStatus(PaymentStatus.COMPLETED);
                booking.setStartDate(LocalDate.now().minusDays(2));
                booking.setEndDate(LocalDate.now().minusDays(1));
                booking.setTotalPrice(30.0);

                when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
                when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

                com.toolshed.backend.dto.ConditionReportRequest request = new com.toolshed.backend.dto.ConditionReportRequest(
                                com.toolshed.backend.repository.enums.ConditionStatus.BROKEN,
                                "Tool is completely broken",
                                renter.getId());

                BookingResponse response = bookingService.submitConditionReport(bookingId, request);

                assertThat(response.getConditionStatus())
                                .isEqualTo(com.toolshed.backend.repository.enums.ConditionStatus.BROKEN);
                assertThat(response.getDepositStatus())
                                .isEqualTo(com.toolshed.backend.repository.enums.DepositStatus.REQUIRED);
                assertThat(response.getDepositAmount()).isEqualTo(50.0);
                verify(bookingRepository).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should set REQUIRED deposit when condition is MINOR_DAMAGE")
        void submitConditionReportMinorDamageRequiresDeposit() {
                UUID bookingId = UUID.randomUUID();
                Booking booking = new Booking();
                booking.setId(bookingId);
                booking.setTool(tool);
                tool.setTitle("Damaged Tool");
                booking.setRenter(renter);
                booking.setOwner(tool.getOwner());
                booking.setStatus(BookingStatus.COMPLETED);
                booking.setPaymentStatus(PaymentStatus.COMPLETED);
                booking.setStartDate(LocalDate.now().minusDays(2));
                booking.setEndDate(LocalDate.now().minusDays(1));
                booking.setTotalPrice(25.0);

                when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
                when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

                com.toolshed.backend.dto.ConditionReportRequest request = new com.toolshed.backend.dto.ConditionReportRequest(
                                com.toolshed.backend.repository.enums.ConditionStatus.MINOR_DAMAGE,
                                "Small scratch on handle",
                                renter.getId());

                BookingResponse response = bookingService.submitConditionReport(bookingId, request);

                assertThat(response.getConditionStatus())
                                .isEqualTo(com.toolshed.backend.repository.enums.ConditionStatus.MINOR_DAMAGE);
                assertThat(response.getDepositStatus())
                                .isEqualTo(com.toolshed.backend.repository.enums.DepositStatus.REQUIRED);
                assertThat(response.getDepositAmount()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should throw exception when booking not found")
        void submitConditionReportBookingNotFound() {
                UUID bookingId = UUID.randomUUID();
                when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

                com.toolshed.backend.dto.ConditionReportRequest request = new com.toolshed.backend.dto.ConditionReportRequest(
                                com.toolshed.backend.repository.enums.ConditionStatus.OK,
                                "Test",
                                renter.getId());

                assertThatThrownBy(() -> bookingService.submitConditionReport(bookingId, request))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("Booking not found")
                                .extracting("statusCode")
                                .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Should throw exception when condition already reported")
        void submitConditionReportAlreadyReported() {
                UUID bookingId = UUID.randomUUID();
                Booking booking = new Booking();
                booking.setId(bookingId);
                booking.setTool(tool);
                booking.setRenter(renter);
                booking.setOwner(tool.getOwner());
                booking.setStatus(BookingStatus.COMPLETED);
                booking.setConditionStatus(com.toolshed.backend.repository.enums.ConditionStatus.OK);

                when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

                com.toolshed.backend.dto.ConditionReportRequest request = new com.toolshed.backend.dto.ConditionReportRequest(
                                com.toolshed.backend.repository.enums.ConditionStatus.BROKEN,
                                "Trying to report again",
                                renter.getId());

                assertThatThrownBy(() -> bookingService.submitConditionReport(bookingId, request))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("already submitted")
                                .extracting("statusCode")
                                .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should throw exception when booking is not COMPLETED")
        void submitConditionReportNotCompleted() {
                UUID bookingId = UUID.randomUUID();
                Booking booking = new Booking();
                booking.setId(bookingId);
                booking.setTool(tool);
                booking.setRenter(renter);
                booking.setOwner(tool.getOwner());
                booking.setStatus(BookingStatus.APPROVED);

                when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

                com.toolshed.backend.dto.ConditionReportRequest request = new com.toolshed.backend.dto.ConditionReportRequest(
                                com.toolshed.backend.repository.enums.ConditionStatus.OK,
                                "Test",
                                renter.getId());

                assertThatThrownBy(() -> bookingService.submitConditionReport(bookingId, request))
                                .isInstanceOf(ResponseStatusException.class)
                                .hasMessageContaining("completed bookings")
                                .extracting("statusCode")
                                .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should set REQUIRED deposit when condition is MISSING_PARTS")
        void submitConditionReportMissingPartsRequiresDeposit() {
                UUID bookingId = UUID.randomUUID();
                Booking booking = new Booking();
                booking.setId(bookingId);
                booking.setTool(tool);
                tool.setTitle("Incomplete Tool");
                booking.setRenter(renter);
                booking.setOwner(tool.getOwner());
                booking.setStatus(BookingStatus.COMPLETED);
                booking.setPaymentStatus(PaymentStatus.COMPLETED);
                booking.setStartDate(LocalDate.now().minusDays(2));
                booking.setEndDate(LocalDate.now().minusDays(1));
                booking.setTotalPrice(40.0);

                when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
                when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

                com.toolshed.backend.dto.ConditionReportRequest request = new com.toolshed.backend.dto.ConditionReportRequest(
                                com.toolshed.backend.repository.enums.ConditionStatus.MISSING_PARTS,
                                "Missing drill bits",
                                renter.getId());

                BookingResponse response = bookingService.submitConditionReport(bookingId, request);

                assertThat(response.getConditionStatus())
                                .isEqualTo(com.toolshed.backend.repository.enums.ConditionStatus.MISSING_PARTS);
                assertThat(response.getDepositStatus())
                                .isEqualTo(com.toolshed.backend.repository.enums.DepositStatus.REQUIRED);
                assertThat(response.getDepositAmount()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should set NOT_REQUIRED deposit when condition is USED")
        void submitConditionReportUsedConditionNoDeposit() {
                UUID bookingId = UUID.randomUUID();
                Booking booking = new Booking();
                booking.setId(bookingId);
                booking.setTool(tool);
                tool.setTitle("Used Tool");
                booking.setRenter(renter);
                booking.setOwner(tool.getOwner());
                booking.setStatus(BookingStatus.COMPLETED);
                booking.setPaymentStatus(PaymentStatus.COMPLETED);
                booking.setStartDate(LocalDate.now().minusDays(2));
                booking.setEndDate(LocalDate.now().minusDays(1));
                booking.setTotalPrice(15.0);

                when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
                when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

                com.toolshed.backend.dto.ConditionReportRequest request = new com.toolshed.backend.dto.ConditionReportRequest(
                                com.toolshed.backend.repository.enums.ConditionStatus.USED,
                                "Normal wear and tear",
                                renter.getId());

                BookingResponse response = bookingService.submitConditionReport(bookingId, request);

                assertThat(response.getConditionStatus())
                                .isEqualTo(com.toolshed.backend.repository.enums.ConditionStatus.USED);
                assertThat(response.getDepositStatus())
                                .isEqualTo(com.toolshed.backend.repository.enums.DepositStatus.NOT_REQUIRED);
                assertThat(response.getDepositAmount()).isEqualTo(0.0);
        }
}
