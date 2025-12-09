package com.toolshed.backend.service;

import com.toolshed.backend.dto.BookingResponse;
import com.toolshed.backend.dto.CreateBookingRequest;
import com.toolshed.backend.dto.OwnerBookingResponse;
import com.toolshed.backend.repository.enums.BookingStatus;

import java.util.List;
import java.util.UUID;

public interface BookingService {
    BookingResponse createBooking(CreateBookingRequest request);
    List<OwnerBookingResponse> getBookingsForOwner(UUID ownerId);
    List<BookingResponse> getBookingsForTool(UUID toolId);
    List<BookingResponse> getBookingsForRenter(UUID renterId);
    BookingResponse updateBookingStatus(UUID bookingId, BookingStatus status);
}
