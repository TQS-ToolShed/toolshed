package com.toolshed.backend.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateToolInput {
    @NotBlank
    private String title;

    @NotBlank
    @Size(max = 1000)
    private String description;

    @NotNull
    @Positive
    private Double pricePerDay;

    @NotNull
    private UUID supplierId;

    @NotBlank
    private String location;
}
