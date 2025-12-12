package com.toolshed.backend.dto;

import java.util.UUID;

import com.toolshed.backend.repository.enums.ReviewType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewRequest {
    @NotNull
    private UUID bookingId;

    private ReviewType type;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @NotBlank
    private String comment;
}
