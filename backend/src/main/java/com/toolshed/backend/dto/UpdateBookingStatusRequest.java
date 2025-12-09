package com.toolshed.backend.dto;

import com.toolshed.backend.repository.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateBookingStatusRequest {
    @NotNull
    private BookingStatus status;
}
