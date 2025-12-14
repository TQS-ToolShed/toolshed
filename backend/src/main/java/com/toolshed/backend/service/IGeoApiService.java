package com.toolshed.backend.service;

import java.util.List;

/**
 * Service interface for interacting with the Portuguese GeoAPI.
 * Provides methods to retrieve and validate districts.
 * Implements caching to avoid hitting API rate limits.
 */
public interface IGeoApiService {
    
    /**
     * Retrieves a list of all districts from the GeoAPI.
     * Results are cached to minimize API calls.
     * @return list of district names.
     */
    List<String> getAllDistricts();
    
    /**
     * Checks if a district exists in the GeoAPI data.
     * @param district The district name to check.
     * @return true if exists, false otherwise.
     */
    boolean districtExists(String district);
}
