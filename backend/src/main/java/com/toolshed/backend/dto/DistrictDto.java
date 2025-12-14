package com.toolshed.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a district from the json.geoapi.pt endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DistrictDto {
    private String distrito; // e.g. "Lisboa"
    private String codigoine; // not used, but mapped for completeness

    public String getDistrito() {
        return distrito;
    }

    public void setDistrito(String distrito) {
        this.distrito = distrito;
    }

    public String getCodigoine() {
        return codigoine;
    }

    public void setCodigoine(String codigoine) {
        this.codigoine = codigoine;
    }
}
