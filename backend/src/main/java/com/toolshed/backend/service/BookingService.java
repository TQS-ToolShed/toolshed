package com.toolshed.backend.service;

import com.toolshed.backend.dto.BookingResponse;
import com.toolshed.backend.dto.CreateBookingRequest;

public interface BookingService {
    BookingResponse createBooking(CreateBookingRequest request);
}
