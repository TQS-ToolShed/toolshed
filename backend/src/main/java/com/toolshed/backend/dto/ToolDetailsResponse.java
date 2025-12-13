package com.toolshed.backend.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolDetailsResponse {
    private UUID id;
    private String title;
    private String description;
    private Double pricePerDay;
    private String location;
    private String district;
    private boolean active;
    private String availabilityCalendar;
    private Double overallRating;
    private int numRatings;
    private OwnerSummary owner;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OwnerSummary {
        private UUID id;
        private String firstName;
        private String lastName;
        private String email;
        private Double reputationScore;
    }
}
