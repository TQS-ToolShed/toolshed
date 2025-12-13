package com.toolshed.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.toolshed.backend.repository.enums.PayoutStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for payout information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutResponse {

    private UUID id;
    private Double amount;
    private PayoutStatus status;
    private String stripeTransferId;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
}
