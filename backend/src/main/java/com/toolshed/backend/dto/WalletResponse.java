package com.toolshed.backend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for owner wallet information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {

    private Double balance;
    private List<PayoutResponse> recentPayouts;
}
