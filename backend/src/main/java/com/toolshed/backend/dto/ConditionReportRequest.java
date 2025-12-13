package com.toolshed.backend.dto;

import java.util.UUID;

import com.toolshed.backend.repository.enums.ConditionStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for submitting a condition report for a completed booking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionReportRequest {

    @NotNull(message = "Condition status is required")
    private ConditionStatus conditionStatus;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Renter ID is required")
    private UUID renterId;
}
