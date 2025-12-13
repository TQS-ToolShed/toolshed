package com.toolshed.backend.service;

import com.toolshed.backend.dto.DistrictResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GeoApiServiceTest {

    @Autowired
    private GeoApiService geoApiService;

    @Test
    void testGetAllDistrictsReturnsNonEmptyList() {
        List<DistrictResponse> districts = geoApiService.getAllDistricts();
        
        assertNotNull(districts, "Districts list should not be null");
        assertFalse(districts.isEmpty(), "Districts list should not be empty");
        assertTrue(districts.size() >= 18, "Should have at least 18 Portuguese districts");
    }

    @Test
    void testGetAllDistrictsContainsLisboa() {
        List<DistrictResponse> districts = geoApiService.getAllDistricts();
        
        boolean hasLisboa = districts.stream()
                .anyMatch(d -> "Lisboa".equals(d.getName()));
        
        assertTrue(hasLisboa, "Districts should contain Lisboa");
    }

    @Test
    void testGetAllDistrictsContainsPorto() {
        List<DistrictResponse> districts = geoApiService.getAllDistricts();
        
        boolean hasPorto = districts.stream()
                .anyMatch(d -> "Porto".equals(d.getName()));
        
        assertTrue(hasPorto, "Districts should contain Porto");
    }

    @Test
    void testAllDistrictsHaveIdAndName() {
        List<DistrictResponse> districts = geoApiService.getAllDistricts();
        
        for (DistrictResponse district : districts) {
            assertNotNull(district.getId(), "District ID should not be null");
            assertNotNull(district.getName(), "District name should not be null");
            assertFalse(district.getId().isEmpty(), "District ID should not be empty");
            assertFalse(district.getName().isEmpty(), "District name should not be empty");
        }
    }
}
