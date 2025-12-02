package com.toolshed.backend.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {
    @NotNull
    private UUID toolId;

    @NotNull
    private UUID renterId;

    @NotNull
    @FutureOrPresent(message = "Start date cannot be in the past")
    private LocalDate startDate;

    @NotNull
    @FutureOrPresent(message = "End date cannot be in the past")
    private LocalDate endDate;
}
