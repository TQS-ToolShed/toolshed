package com.toolshed.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolshed.backend.dto.DistrictResponse;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeoApiService {
    
    private static final Logger logger = LoggerFactory.getLogger(GeoApiService.class);
    private static final String GEO_API_BASE_URL = "https://geoapi.pt";
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    // In-memory cache for districts
    private List<DistrictResponse> cachedDistricts = new ArrayList<>();
    
    public GeoApiService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Initialize the cache on application startup.
     * This method is called automatically after the bean is constructed.
     */
    @PostConstruct
    public void initializeCache() {
        logger.info("Initializing district cache from GeoAPI...");
        try {
            fetchAndCacheDistricts();
            logger.info("Successfully cached {} districts", cachedDistricts.size());
        } catch (Exception e) {
            logger.warn("Failed to fetch districts from GeoAPI, using fallback data: {}", e.getMessage());
            useFallbackDistricts();
        }
    }
    
    /**
     * Fetch districts from the Portuguese GeoAPI and cache them.
     */
    private void fetchAndCacheDistricts() throws Exception {
        String url = GEO_API_BASE_URL + "/distrito";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            // Parse the response - assuming it's a JSON array or object
            List<Map<String, Object>> apiDistricts = objectMapper.readValue(
                    response.body(), 
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            
            cachedDistricts = new ArrayList<>();
            for (Map<String, Object> district : apiDistricts) {
                String id = district.get("id") != null ? district.get("id").toString() : "";
                String name = district.get("nome") != null ? district.get("nome").toString() : 
                             district.get("name") != null ? district.get("name").toString() : "";
                
                if (!id.isEmpty() && !name.isEmpty()) {
                    cachedDistricts.add(DistrictResponse.builder()
                            .id(id)
                            .name(name)
                            .build());
                }
            }
        } else {
            throw new RuntimeException("GeoAPI returned status code: " + response.statusCode());
        }
    }
    
    /**
     * Use fallback Portuguese districts if API is unavailable.
     * These are the 18 districts of mainland Portugal + 2 autonomous regions.
     */
    private void useFallbackDistricts() {
        cachedDistricts = new ArrayList<>();
        String[] portugueseDistricts = {
            "Aveiro", "Beja", "Braga", "Bragança", "Castelo Branco",
            "Coimbra", "Évora", "Faro", "Guarda", "Leiria",
            "Lisboa", "Portalegre", "Porto", "Santarém", "Setúbal",
            "Viana do Castelo", "Vila Real", "Viseu",
            "Região Autónoma dos Açores", "Região Autónoma da Madeira"
        };
        
        for (int i = 0; i < portugueseDistricts.length; i++) {
            cachedDistricts.add(DistrictResponse.builder()
                    .id(String.valueOf(i + 1))
                    .name(portugueseDistricts[i])
                    .build());
        }
        logger.info("Using fallback data with {} districts", cachedDistricts.size());
    }
    
    /**
     * Get all cached districts.
     * @return List of all districts (never null)
     */
    public List<DistrictResponse> getAllDistricts() {
        return new ArrayList<>(cachedDistricts);
    }
    
    /**
     * Manually refresh the district cache.
     * This can be called if the cache needs to be updated.
     */
    public void refreshCache() {
        logger.info("Manually refreshing district cache...");
        initializeCache();
    }
}
