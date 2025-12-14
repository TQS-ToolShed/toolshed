package com.toolshed.backend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for owner earnings breakdown.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerEarningsResponse {
    
    private List<MonthlyEarnings> monthlyEarnings;
    private Double totalEarnings;
}
