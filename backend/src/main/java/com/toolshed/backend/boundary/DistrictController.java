package com.toolshed.backend.boundary;

import com.toolshed.backend.dto.DistrictResponse;
import com.toolshed.backend.service.GeoApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/districts")
public class DistrictController {
    
    private final GeoApiService geoApiService;
    
    @Autowired
    public DistrictController(GeoApiService geoApiService) {
        this.geoApiService = geoApiService;
    }
    
    /**
     * Get all available districts.
     * This returns cached data to avoid hitting the external API.
     */
    @GetMapping
    public ResponseEntity<List<DistrictResponse>> getAllDistricts() {
        List<DistrictResponse> districts = geoApiService.getAllDistricts();
        return ResponseEntity.ok(districts);
    }
}
