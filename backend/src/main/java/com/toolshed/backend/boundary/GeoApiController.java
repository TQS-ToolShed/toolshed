package com.toolshed.backend.boundary;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toolshed.backend.service.IGeoApiService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "GeoAPI", description = "Operations for retrieving Portuguese districts and municipalities")
@RestController
@RequestMapping("/api/geo")
public class GeoApiController {

    private final IGeoApiService geoApiService;

    public GeoApiController(IGeoApiService geoApiService) {
        this.geoApiService = geoApiService;
    }

    @Operation(
        summary = "Get all districts",
        description = "Retrieves a list of all Portuguese districts from the GeoAPI. Results are cached.",
        responses = {
            @ApiResponse(
                responseCode = "200", 
                description = "Successfully retrieved list of districts"
            )
        }
    )
    @GetMapping("/districts")
    public ResponseEntity<List<String>> getAllDistricts() {
        return ResponseEntity.ok(geoApiService.getAllDistricts());
    }

    @Operation(
        summary = "Get municipalities by district",
        description = "Retrieves a list of all municipalities for a given district. Results are cached.",
        responses = {
            @ApiResponse(
                responseCode = "200", 
                description = "Successfully retrieved list of municipalities"
            )
        }
    )
    @GetMapping("/districts/{district}/municipalities")
    public ResponseEntity<List<String>> getMunicipalitiesByDistrict(
            @Parameter(description = "District name (e.g., 'Aveiro')")
            @PathVariable String district) {
        return ResponseEntity.ok(geoApiService.getMunicipalitiesByDistrict(district));
    }
}
