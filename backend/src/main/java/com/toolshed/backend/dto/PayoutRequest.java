package com.toolshed.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for payout requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayoutRequest {

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;
}
