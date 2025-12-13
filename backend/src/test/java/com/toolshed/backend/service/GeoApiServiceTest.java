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
    private static final String AVEIRO_MUNICIPALITIES_JSON = "{\"distrito\":\"Aveiro\",\"municipios\":[{\"nome\":\"Águeda\",\"codigoine\":\"0101\"},{\"nome\":\"Albergaria-a-Velha\",\"codigoine\":\"0102\"},{\"nome\":\"Aveiro\",\"codigoine\":\"0103\"}]}";
    private static final String LISBOA_MUNICIPALITIES_JSON = "{\"distrito\":\"Lisboa\",\"municipios\":[{\"nome\":\"Lisboa\",\"codigoine\":\"1101\"},{\"nome\":\"Loures\",\"codigoine\":\"1102\"},{\"nome\":\"Sintra\",\"codigoine\":\"1103\"}]}";

    @BeforeEach
    void setUp() throws IOException {
        // Isolate disk cache between tests
        System.setProperty("geo.cache.path", tempDir.resolve("geo-cache.json").toString());
        geoApiService = new GeoApiService(httpClient);

        // Setup default mock responses using lenient to avoid unnecessary stubbing errors
        lenient().when(httpClient.doHttpGet("https://json.geoapi.pt/distritos")).thenReturn(DISTRICTS_JSON);
        lenient().when(httpClient.doHttpGet("https://json.geoapi.pt/distrito/Aveiro/municipios")).thenReturn(AVEIRO_MUNICIPALITIES_JSON);
        lenient().when(httpClient.doHttpGet("https://json.geoapi.pt/distrito/Lisboa/municipios")).thenReturn(LISBOA_MUNICIPALITIES_JSON);
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
    @DisplayName("Should fetch municipalities for a given district")
    void testGetMunicipalitiesByDistrict() throws IOException {
        // Act
        List<String> municipalities = geoApiService.getMunicipalitiesByDistrict("Aveiro");

        // Assert
        assertThat(municipalities).hasSize(3);
        assertThat(municipalities).contains("Águeda", "Albergaria-a-Velha", "Aveiro");
        verify(httpClient, times(1)).doHttpGet("https://json.geoapi.pt/distrito/Aveiro/municipios");
    }

    @Test
    @DisplayName("Should cache municipalities per district")
    void testGetMunicipalitiesByDistrictCaching() throws IOException {
        // Act
        List<String> municipalities1 = geoApiService.getMunicipalitiesByDistrict("Aveiro");
        List<String> municipalities2 = geoApiService.getMunicipalitiesByDistrict("Aveiro");
        List<String> lisboaMunicipalities = geoApiService.getMunicipalitiesByDistrict("Lisboa");

        // Assert
        assertThat(municipalities1).isEqualTo(municipalities2);
        assertThat(lisboaMunicipalities).hasSize(3);
        assertThat(lisboaMunicipalities).contains("Lisboa", "Loures", "Sintra");
        
        // Should only call API once per district
        verify(httpClient, times(1)).doHttpGet("https://json.geoapi.pt/distrito/Aveiro/municipios");
        verify(httpClient, times(1)).doHttpGet("https://json.geoapi.pt/distrito/Lisboa/municipios");
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
    @DisplayName("Should return true when municipality exists in district")
    void testMunicipalityExistsInDistrict() throws IOException {
        // Act & Assert
        assertThat(geoApiService.municipalityExistsInDistrict("Aveiro", "Aveiro")).isTrue();
        assertThat(geoApiService.municipalityExistsInDistrict("Aveiro", "Águeda")).isTrue();
        assertThat(geoApiService.municipalityExistsInDistrict("Lisboa", "Lisboa")).isTrue();
    }

    @Test
    @DisplayName("Should return false when municipality does not exist in district")
    void testMunicipalityDoesNotExistInDistrict() throws IOException {
        // Act & Assert
        assertThat(geoApiService.municipalityExistsInDistrict("Aveiro", "Lisboa")).isFalse();
        assertThat(geoApiService.municipalityExistsInDistrict("Aveiro", "InvalidMunicipality")).isFalse();
    }

    @Test
    @DisplayName("Should return false when checking municipality for non-existent district")
    void testMunicipalityCheckForInvalidDistrict() throws IOException {
        // Act & Assert
        assertThat(geoApiService.municipalityExistsInDistrict("InvalidDistrict", "SomeMunicipality")).isFalse();
    }

    @Test
    @DisplayName("Should return empty list when API returns error for municipalities")
    void testGetMunicipalitiesWithApiError() throws IOException {
        // Arrange
        when(httpClient.doHttpGet("https://json.geoapi.pt/distrito/ErrorDistrict/municipios")).thenThrow(new IOException("API Error"));

        // Act
        List<String> municipalities = geoApiService.getMunicipalitiesByDistrict("ErrorDistrict");

        // Assert
        assertThat(municipalities).isEmpty();
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
