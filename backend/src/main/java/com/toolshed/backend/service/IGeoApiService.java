package com.toolshed.backend.service;

import java.util.List;

/**
 * Service interface for interacting with the Portuguese GeoAPI.
 * Provides methods to retrieve and validate districts and municipalities.
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
     * Retrieves a list of all municipalities for a given district.
     * Results are cached to minimize API calls.
     * @param district The district name.
     * @return list of municipality names for the district.
     */
    List<String> getMunicipalitiesByDistrict(String district);
    
    /**
     * Checks if a district exists in the GeoAPI data.
     * @param district The district name to check.
     * @return true if exists, false otherwise.
     */
    boolean districtExists(String district);
    
    /**
     * Checks if a municipality exists in the given district.
     * @param district The district name.
     * @param municipality The municipality name to check.
     * @return true if the municipality exists in the district, false otherwise.
     */
    boolean municipalityExistsInDistrict(String district, String municipality);
}
