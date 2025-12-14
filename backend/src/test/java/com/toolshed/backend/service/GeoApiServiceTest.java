package com.toolshed.backend.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GeoApiServiceTest {

    @Mock
    private ISimpleHttpClient httpClient;

    private GeoApiService geoApiService;

    @TempDir
    Path tempDir;

    private static final String DISTRICTS_JSON = "[{\"distrito\":\"Aveiro\"},{\"distrito\":\"Beja\"},{\"distrito\":\"Lisboa\"}]";

    @BeforeEach
    void setUp() throws IOException {
        // Isolate disk cache between tests
        System.setProperty("geo.cache.path", tempDir.resolve("geo-cache.json").toString());
        geoApiService = new GeoApiService(httpClient);

        // Setup default mock responses using lenient to avoid unnecessary stubbing errors
        lenient().when(httpClient.doHttpGet("https://json.geoapi.pt/distritos")).thenReturn(DISTRICTS_JSON);
    }

    @Test
    @DisplayName("Should fetch all districts from API")
    void testGetAllDistricts() throws IOException {
        // Act
        List<String> districts = geoApiService.getAllDistricts();

        // Assert
        assertThat(districts).hasSize(3);
        assertThat(districts).contains("Aveiro", "Beja", "Lisboa");
        verify(httpClient, times(1)).doHttpGet("https://json.geoapi.pt/distritos");
    }

    @Test
    @DisplayName("Should cache districts and not call API twice")
    void testGetAllDistrictsCaching() throws IOException {
        // Act
        List<String> districts1 = geoApiService.getAllDistricts();
        List<String> districts2 = geoApiService.getAllDistricts();

        // Assert
        assertThat(districts1).isEqualTo(districts2);
        verify(httpClient, times(1)).doHttpGet("https://json.geoapi.pt/distritos");
    }

    @Test
    @DisplayName("Should return true when district exists")
    void testDistrictExists() throws IOException {
        // Act & Assert
        assertThat(geoApiService.districtExists("Aveiro")).isTrue();
        assertThat(geoApiService.districtExists("Lisboa")).isTrue();
        assertThat(geoApiService.districtExists("Beja")).isTrue();
    }

    @Test
    @DisplayName("Should return false when district does not exist")
    void testDistrictDoesNotExist() throws IOException {
        // Act & Assert
        assertThat(geoApiService.districtExists("InvalidDistrict")).isFalse();
        assertThat(geoApiService.districtExists("Porto")).isFalse();
    }

    @Test
    @DisplayName("Should return empty list when API returns error for districts")
    void testGetDistrictsWithApiError() throws IOException {
        // Arrange
        when(httpClient.doHttpGet("https://json.geoapi.pt/distritos")).thenThrow(new IOException("API Error"));

        // Act
        List<String> districts = geoApiService.getAllDistricts();

        // Assert
        assertThat(districts).isEmpty();
    }
}
