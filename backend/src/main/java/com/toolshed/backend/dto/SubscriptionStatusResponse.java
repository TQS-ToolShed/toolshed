package com.toolshed.backend.dto;

import com.toolshed.backend.repository.enums.SubscriptionTier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for subscription status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionStatusResponse {
    private SubscriptionTier tier;
    private boolean active;
    private LocalDateTime subscriptionStart;
    private LocalDateTime subscriptionEnd;
    private Double discountPercentage;
}
