package com.toolshed.backend.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.toolshed.backend.dto.BookingResponse;
import com.toolshed.backend.dto.CreateBookingRequest;
import com.toolshed.backend.dto.OwnerBookingResponse;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.PaymentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ToolRepository toolRepository;
    private final UserRepository userRepository;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              ToolRepository toolRepository,
                              UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.toolRepository = toolRepository;
        this.userRepository = userRepository;
    }

    @Override
    public BookingResponse createBooking(CreateBookingRequest request) {
        LocalDate today = LocalDate.now();
        if (request.getStartDate().isBefore(today) || request.getEndDate().isBefore(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dates cannot be in the past");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must be on or after start date");
        }

        Tool tool = toolRepository.findById(request.getToolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tool not found"));

        User renter = userRepository.findById(request.getRenterId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Renter not found"));

        User owner = tool.getOwner();
        if (owner == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tool owner is missing");
        }

        List<Booking> overlaps = bookingRepository.findOverlappingBookings(
                tool.getId(), request.getStartDate(), request.getEndDate());
        if (!overlaps.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tool is already booked for these dates");
        }

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        double totalPrice = days * tool.getPricePerDay();

        Booking booking = new Booking();
        booking.setTool(tool);
        booking.setRenter(renter);
        booking.setOwner(owner);
        booking.setStartDate(request.getStartDate());
        booking.setEndDate(request.getEndDate());
        booking.setStatus(BookingStatus.PENDING);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setTotalPrice(totalPrice);

        Booking saved = bookingRepository.save(booking);

        return toBookingResponse(saved);
    }

    /**
     * task to mark finished bookings as completed and free tools if they are no longer rented.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void completeExpiredBookings() {
        LocalDate today = LocalDate.now();
        List<Booking> expired = bookingRepository.findByStatusAndEndDateBefore(BookingStatus.APPROVED, today);

        for (Booking booking : expired) {
            booking.setStatus(BookingStatus.COMPLETED);
            bookingRepository.save(booking);

            Tool tool = booking.getTool();
            if (tool != null) {
                long activeCount = bookingRepository.countActiveApprovedBookingsForToolOnDate(tool.getId(), today);
                tool.setActive(activeCount == 0);
                toolRepository.save(tool);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OwnerBookingResponse> getBookingsForOwner(UUID ownerId) {
        List<Booking> bookings = bookingRepository.findByOwnerId(ownerId);
        return bookings.stream()
                .map(this::toOwnerBookingResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsForTool(UUID toolId) {
        List<Booking> bookings = bookingRepository.findByToolId(toolId);
        return bookings.stream()
                .map(this::toBookingResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsForRenter(UUID renterId) {
        List<Booking> bookings = bookingRepository.findByRenterId(renterId);
        return bookings.stream()
                .map(this::toBookingResponse)
                .toList();
    }

    @Override
    @Transactional
    public BookingResponse updateBookingStatus(UUID bookingId, BookingStatus status) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (status != BookingStatus.APPROVED && status != BookingStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only approval or rejection is supported");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking decision is already final");
        }

        booking.setStatus(status);

        // If approved, only mark the tool unavailable during the booking window
        if (status == BookingStatus.APPROVED) {
            List<Booking> overlaps = bookingRepository.findOverlappingBookings(
                    booking.getTool().getId(),
                    booking.getStartDate(),
                    booking.getEndDate());

            boolean hasApprovedOverlap = overlaps.stream()
                    .anyMatch(b -> !b.getId().equals(bookingId) && b.getStatus() == BookingStatus.APPROVED);
            if (hasApprovedOverlap) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking window already approved for this tool");
            }

            Tool tool = booking.getTool();
            if (tool != null) {
                LocalDate today = LocalDate.now();
                boolean withinWindow = !today.isBefore(booking.getStartDate()) && !today.isAfter(booking.getEndDate());
                tool.setActive(!withinWindow);
                toolRepository.save(tool);
            }
        }

        Booking saved = bookingRepository.save(booking);

        return toBookingResponse(saved);
    }

    private OwnerBookingResponse toOwnerBookingResponse(Booking booking) {
        Tool tool = booking.getTool();
        User renter = booking.getRenter();
        String renterName = renter != null
                ? (renter.getFirstName() + " " + renter.getLastName()).trim()
                : null;

        return OwnerBookingResponse.builder()
                .id(booking.getId())
                .toolId(tool != null ? tool.getId() : null)
                .toolTitle(tool != null ? tool.getTitle() : null)
                .renterId(renter != null ? renter.getId() : null)
                .renterName(renterName)
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .status(booking.getStatus())
                .totalPrice(booking.getTotalPrice())
                .build();
    }

    private BookingResponse toBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .toolId(booking.getTool().getId())
                .renterId(booking.getRenter().getId())
                .ownerId(booking.getOwner().getId())
                .toolTitle(booking.getTool().getTitle() != null ? booking.getTool().getTitle() : null)
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .status(booking.getStatus())
                .paymentStatus(booking.getPaymentStatus())
                .totalPrice(booking.getTotalPrice())
                .build();
    }
}
