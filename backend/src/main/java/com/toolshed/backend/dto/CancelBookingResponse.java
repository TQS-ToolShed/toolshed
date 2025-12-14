package com.toolshed.backend.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for booking cancellation with refund details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelBookingResponse {

    private UUID bookingId;
    private String status;
    private Double refundAmount;
    private Integer refundPercentage;
    private String message;
}
