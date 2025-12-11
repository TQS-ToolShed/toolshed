package com.toolshed.backend.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolshed.backend.dto.DistrictDto;
import com.toolshed.backend.dto.MunicipalityDto;

/**
 * Service implementation for interacting with the Portuguese GeoAPI.
 * Implements caching to minimize API calls and avoid rate limits.
 */
@Service
public class GeoApiService implements IGeoApiService {

    private static final String GEO_API_BASE_URL = "https://geoapi.pt";
    private static final String DISTRICTS_ENDPOINT = "/distritos";
    private static final String MUNICIPALITIES_ENDPOINT = "/distrito/%s/municipios";

    private final ISimpleHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    // Cache for districts (initialized as null, loaded on first request)
    private List<String> cachedDistricts = null;
    
    // Cache for municipalities per district
    private final Map<String, List<String>> cachedMunicipalities = new HashMap<>();

    public GeoApiService(ISimpleHttpClient httpClient) {
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<String> getAllDistricts() {
        if (cachedDistricts != null) {
            return new ArrayList<>(cachedDistricts);
        }

        try {
            String url = GEO_API_BASE_URL + DISTRICTS_ENDPOINT;
            String response = httpClient.doHttpGet(url);
            
            List<DistrictDto> districts = objectMapper.readValue(
                response, 
                new TypeReference<List<DistrictDto>>() {}
            );
            
            cachedDistricts = districts.stream()
                .map(DistrictDto::getNome)
                .collect(Collectors.toList());
            
            return new ArrayList<>(cachedDistricts);
        } catch (IOException e) {
            // Return empty list on error
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> getMunicipalitiesByDistrict(String district) {
        if (cachedMunicipalities.containsKey(district)) {
            return new ArrayList<>(cachedMunicipalities.get(district));
        }

        try {
            String url = GEO_API_BASE_URL + String.format(MUNICIPALITIES_ENDPOINT, district);
            String response = httpClient.doHttpGet(url);
            
            List<MunicipalityDto> municipalities = objectMapper.readValue(
                response,
                new TypeReference<List<MunicipalityDto>>() {}
            );
            
            List<String> municipalityNames = municipalities.stream()
                .map(MunicipalityDto::getNome)
                .collect(Collectors.toList());
            
            cachedMunicipalities.put(district, municipalityNames);
            
            return new ArrayList<>(municipalityNames);
        } catch (IOException e) {
            // Return empty list on error
            return new ArrayList<>();
        }
    }

    @Override
    public boolean districtExists(String district) {
        List<String> districts = getAllDistricts();
        return districts.contains(district);
    }

    @Override
    public boolean municipalityExistsInDistrict(String district, String municipality) {
        if (!districtExists(district)) {
            return false;
        }
        
        List<String> municipalities = getMunicipalitiesByDistrict(district);
        return municipalities.contains(municipality);
    }
}
