package com.toolshed.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a municipality from the GeoAPI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MunicipalityDto {
    private String id;
    private String nome;
}
