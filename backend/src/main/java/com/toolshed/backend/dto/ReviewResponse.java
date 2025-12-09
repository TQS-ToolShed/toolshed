package com.toolshed.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private UUID id;
    private UUID bookingId;
    private UUID reviewerId;
    private String reviewerName;
    private UUID ownerId;
    private UUID toolId;
    private Integer rating;
    private String comment;
    private LocalDateTime date;
}
