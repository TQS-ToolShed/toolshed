package com.toolshed.backend.boundary;

import com.toolshed.backend.service.GeoApiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DistrictControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GeoApiService geoApiService;

    @Test
    void testGetAllDistrictsReturns200() throws Exception {
        mockMvc.perform(get("/api/districts"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    void testGetAllDistrictsReturnsArray() throws Exception {
        mockMvc.perform(get("/api/districts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetAllDistrictsReturnsNonEmptyArray() throws Exception {
        mockMvc.perform(get("/api/districts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").exists());
    }
}
