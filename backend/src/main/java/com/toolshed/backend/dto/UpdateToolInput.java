package com.toolshed.backend.dto;

import java.util.UUID;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 DTO used by the PUT endpoint. Fields are nullable so the controller can perform
 partial updates: only non-null fields from the request will be applied.
*/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateToolInput {
    private String title;

    @Size(max = 1000)
    private String description;

    private Double pricePerDay;

    private String location;

    private UUID ownerId;

    private Boolean active;

    private String availabilityCalendar;

    private Double overallRating;

    private Integer numRatings;
}
