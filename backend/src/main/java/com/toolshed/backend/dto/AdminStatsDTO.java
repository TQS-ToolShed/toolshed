package com.toolshed.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsDTO {
    private long totalUsers;
    private long activeUsers;
    private long inactiveUsers;
    private long totalBookings;
    private long activeBookings;
    private long completedBookings;
    private long cancelledBookings;
    private double totalRevenue; // Optional: total transaction volume
}
