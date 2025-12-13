package com.toolshed.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolshed.backend.dto.DistrictDto;

/**
 * Service implementation for interacting with the Portuguese GeoAPI.
 * Implements caching to minimize API calls and avoid rate limits.
 */
@Service
public class GeoApiService implements IGeoApiService {

    private static final String GEO_API_BASE_URL = "https://json.geoapi.pt";
    private static final String DISTRICTS_ENDPOINT = "/distritos";

    private final ISimpleHttpClient httpClient;
    private final ObjectMapper objectMapper;

    private final Path cacheFilePath;
    
    // Cache for districts (initialized as null, loaded on first request)
    private List<String> cachedDistricts = null;

    public GeoApiService(ISimpleHttpClient httpClient) {
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();

        // Persist cache across restarts to avoid hitting GeoAPI repeatedly in dev.
        // Can be configured via env var/property GEO_CACHE_PATH / geo.cache.path
        String configuredPath = System.getProperty("geo.cache.path");
        if (configuredPath == null || configuredPath.isBlank()) {
            configuredPath = System.getenv("GEO_CACHE_PATH");
        }
        if (configuredPath == null || configuredPath.isBlank()) {
            configuredPath = "./geo-cache.json";
        }
        this.cacheFilePath = Paths.get(configuredPath);

        loadCacheFromDisk();
    }

    private static class GeoCacheFileDto {
        public List<String> districts;
    }

    private synchronized void loadCacheFromDisk() {
        try {
            if (!Files.exists(cacheFilePath)) {
                return;
            }

            try (InputStream in = Files.newInputStream(cacheFilePath)) {
                GeoCacheFileDto dto = objectMapper.readValue(in, GeoCacheFileDto.class);

                if (dto != null) {
                    if (dto.districts != null && !dto.districts.isEmpty()) {
                        this.cachedDistricts = new ArrayList<>(dto.districts);
                    }
                }
            }
        } catch (IOException e) {
            // Ignore disk cache errors; service will fall back to live GeoAPI
        }
    }

    private synchronized void saveCacheToDisk() {
        try {
            Path parent = cacheFilePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            GeoCacheFileDto dto = new GeoCacheFileDto();
            dto.districts = cachedDistricts == null ? null : new ArrayList<>(cachedDistricts);

            try (OutputStream out = Files.newOutputStream(cacheFilePath)) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(out, dto);
            }
        } catch (IOException e) {
            // Ignore disk cache errors
        }
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

            // Use correct getter for 'distrito' property
            cachedDistricts = districts.stream()
                .map(DistrictDto::getDistrito)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.toList());

            saveCacheToDisk();

            return new ArrayList<>(cachedDistricts);
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
}
