package com.toolshed.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
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
    private static final String MUNICIPALITIES_ENDPOINT = "/distrito/%s/municipios";

    private final ISimpleHttpClient httpClient;
    private final ObjectMapper objectMapper;

    private final Path cacheFilePath;
    
    // Cache for districts (initialized as null, loaded on first request)
    private List<String> cachedDistricts = null;
    
    // Cache for municipalities per district
    private final Map<String, List<String>> cachedMunicipalities = new HashMap<>();

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
        public Map<String, List<String>> municipalities;
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
                    if (dto.municipalities != null && !dto.municipalities.isEmpty()) {
                        this.cachedMunicipalities.clear();
                        dto.municipalities.forEach((district, municipalities) -> {
                            if (district == null || district.isBlank() || municipalities == null || municipalities.isEmpty()) {
                                return;
                            }
                            this.cachedMunicipalities.put(district, new ArrayList<>(municipalities));
                        });
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
            dto.municipalities = new HashMap<>(cachedMunicipalities);

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
    public List<String> getMunicipalitiesByDistrict(String district) {
        if (cachedMunicipalities.containsKey(district)) {
            return new ArrayList<>(cachedMunicipalities.get(district));
        }

        try {
            // URL-encode the district name to handle spaces and special characters
            String encodedDistrict = URLEncoder.encode(district, StandardCharsets.UTF_8.toString());
            String url = GEO_API_BASE_URL + String.format(MUNICIPALITIES_ENDPOINT, encodedDistrict);
            String response = httpClient.doHttpGet(url);

            JsonNode root = objectMapper.readTree(response);
            JsonNode municipiosNode = root == null ? null : root.get("municipios");

            if (municipiosNode == null || !municipiosNode.isArray()) {
                // Avoid poisoning cache with invalid responses (e.g., rate-limit payloads)
                return new ArrayList<>();
            }

            List<String> municipalityNames = new ArrayList<>();
            for (JsonNode municipio : municipiosNode) {
                if (municipio == null || municipio.isNull()) {
                    continue;
                }
                if (municipio.isTextual()) {
                    String name = municipio.asText();
                    if (name != null && !name.isBlank()) {
                        municipalityNames.add(name);
                    }
                    continue;
                }

                if (municipio.isObject()) {
                    JsonNode nameNode = municipio.get("nome");
                    if (nameNode != null && nameNode.isTextual()) {
                        String name = nameNode.asText();
                        if (name != null && !name.isBlank()) {
                            municipalityNames.add(name);
                        }
                    }
                }
            }

            if (municipalityNames.isEmpty()) {
                // Don't persist empty lists; they're almost certainly invalid/partial data
                return new ArrayList<>();
            }

            cachedMunicipalities.put(district, new ArrayList<>(municipalityNames));
            saveCacheToDisk();
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
