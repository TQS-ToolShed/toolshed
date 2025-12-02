package com.toolshed.backend.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.toolshed.backend.dto.BookingResponse;
import com.toolshed.backend.dto.CreateBookingRequest;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.PaymentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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

        return BookingResponse.builder()
                .id(saved.getId())
                .toolId(saved.getTool().getId())
                .renterId(saved.getRenter().getId())
                .ownerId(saved.getOwner().getId())
                .startDate(saved.getStartDate())
                .endDate(saved.getEndDate())
                .status(saved.getStatus())
                .paymentStatus(saved.getPaymentStatus())
                .totalPrice(saved.getTotalPrice())
                .build();
    }
}
