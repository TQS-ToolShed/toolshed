package com.toolshed.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing earnings for a specific month.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyEarnings {
    
    private Integer year;
    private Integer month;
    private Double totalEarnings;
    private Long bookingCount;
}
