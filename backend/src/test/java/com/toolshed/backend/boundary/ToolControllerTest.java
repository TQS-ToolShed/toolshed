package com.toolshed.backend.boundary;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolshed.backend.dto.CreateToolInput;
import com.toolshed.backend.dto.UpdateToolInput;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.service.ToolService;

@WebMvcTest(ToolController.class) //
class ToolControllerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean 
    private ToolService toolService;

    private Tool createSampleTool(String title) {
        Tool tool = new Tool();
        tool.setId(UUID.randomUUID());
        tool.setTitle(title);
        tool.setDescription("A basic tool");
        tool.setActive(true);
        tool.setPricePerDay(5.0);
        tool.setDistrict("Lisboa");
        tool.setOverallRating(4.5);
        tool.setNumRatings(10);

        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setFirstName("Alice");
        owner.setLastName("Builder");
        owner.setEmail("alice@example.com");
        owner.setReputationScore(4.9);
        tool.setOwner(owner);
        // NOTE: In a real test, you would set all mandatory fields including the Owner
        return tool;
    }

    @Test
    @DisplayName("Should pass both Keyword AND Location to service")
    void testSearchWithKeywordAndLocation() throws Exception {
        // Arrange
        String keyword = "drill";
        String location = "Downtown";
        Tool drill = createSampleTool("Downtown Drill");

        // Mock the service to expect BOTH arguments
        when(toolService.searchTools(keyword, location, null, null)).thenReturn(List.of(drill));

        // Act & Assert
        mockMvc.perform(get("/api/tools/search")
                .param("keyword", keyword)
                .param("location", location) 
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Downtown Drill")));

        // Verification: Ensure location was NOT null
        verify(toolService, times(1)).searchTools(keyword, location, null, null);
    }

    @Test
    @DisplayName("Should return 200 OK and tools for a specific supplier")
    void testGetToolsBySupplier() throws Exception {
        // Arrange
        String keyword = "drill";
        Tool drill = createSampleTool("Power Drill");
        UUID supplierId = UUID.randomUUID();

        // Mock: expect null for location because we don't send the param
        when(toolService.searchTools(keyword, null, null, null)).thenReturn(List.of(drill));

        // Act & Assert
        mockMvc.perform(get("/api/tools/supplier/{supplierId}", supplierId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is("Tool 1")))
                .andExpect(jsonPath("$[1].title", is("Tool 2")));

        // Verification
        verify(toolService, times(1)).searchTools(keyword, null, null, null);
    }

    @Test
    @DisplayName("Should return 200 OK and an empty array when no results are found")
    void testSearchTools_NoMatches_ReturnsEmptyArray() throws Exception {
        // Arrange
        String keyword = "unicorn";

        when(toolService.searchTools(keyword, null, null, null)).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/tools/search")
                .param("keyword", keyword)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        // Verification
        verify(toolService, times(1)).searchTools(keyword, null, null, null);
    }

    @Test
    @DisplayName("Should handle missing keyword parameter gracefully")
    void testSearchTools_MissingParameter_ReturnsEmptyArray() throws Exception {
        // Arrange
        when(toolService.searchTools(null, null, null, null)).thenReturn(Collections.emptyList());

        // Act & Assert
        // Calling endpoint with NO parameters
        mockMvc.perform(get("/api/tools/search")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        // Verification: Ensure service called with nulls
        verify(toolService, times(1)).searchTools(null, null, null, null);
    }

    @Test
    @DisplayName("Should return tool details including owner summary for GET /api/tools/{id}")
    void testGetToolByIdReturnsDetails() throws Exception {
        Tool tool = createSampleTool("Detail Drill");
        tool.setAvailabilityCalendar("2024-09: available weekdays");

        when(toolService.getById(tool.getId())).thenReturn(Optional.of(tool));

        mockMvc.perform(get("/api/tools/{toolId}", tool.getId().toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(tool.getId().toString())))
                .andExpect(jsonPath("$.title", is("Detail Drill")))
                .andExpect(jsonPath("$.pricePerDay", is(tool.getPricePerDay())))
                .andExpect(jsonPath("$.district", is(tool.getDistrict())))
                .andExpect(jsonPath("$.availabilityCalendar", is(tool.getAvailabilityCalendar())))
                .andExpect(jsonPath("$.overallRating", is(tool.getOverallRating())))
                .andExpect(jsonPath("$.numRatings", is(tool.getNumRatings())))
                .andExpect(jsonPath("$.owner.id", is(tool.getOwner().getId().toString())))
                .andExpect(jsonPath("$.owner.firstName", is(tool.getOwner().getFirstName())))
                .andExpect(jsonPath("$.owner.lastName", is(tool.getOwner().getLastName())))
                .andExpect(jsonPath("$.owner.email", is(tool.getOwner().getEmail())))
                .andExpect(jsonPath("$.owner.reputationScore", is(tool.getOwner().getReputationScore())));

        verify(toolService).getById(tool.getId());
    }

    @Test
    @DisplayName("Should return 404 when tool is not found")
    void testGetToolByIdNotFound() throws Exception {
        UUID missingId = UUID.randomUUID();
        when(toolService.getById(missingId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/tools/{toolId}", missingId.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(notNullValue()));

        verify(toolService).getById(missingId);
    }

    @Test
    @DisplayName("Should return active tools list")
    void testGetActiveTools() throws Exception {
        Tool activeTool = createSampleTool("Active Saw");
        activeTool.setActive(true);

        when(toolService.getActive()).thenReturn(List.of(activeTool));

        mockMvc.perform(get("/api/tools/active").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Active Saw")));

        verify(toolService).getActive();
    }

    @Test
    @DisplayName("Should return all tools")
    void testGetAllTools() throws Exception {
        Tool tool = createSampleTool("Any Tool");
        when(toolService.getAll()).thenReturn(List.of(tool));

        mockMvc.perform(get("/api/tools")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Any Tool")));

        verify(toolService).getAll();
    }

    @Test
    @DisplayName("Should create tool and return 200")
    void testCreateTool() throws Exception {
        CreateToolInput input = CreateToolInput.builder()
                .title("New Tool")
                .description("Desc")
                .pricePerDay(10.0)
                .district("Aveiro")
                .supplierId(UUID.randomUUID())
                .build();

        when(toolService.createTool(any(CreateToolInput.class))).thenReturn("new-tool-id");

        mockMvc.perform(post("/api/tools")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated());

        verify(toolService).createTool(any(CreateToolInput.class));
    }

    @Test
    @DisplayName("Should update tool and return 200")
    void testUpdateTool() throws Exception {
        UpdateToolInput input = UpdateToolInput.builder()
                .title("Updated Tool")
                .build();
        
        String toolId = UUID.randomUUID().toString();

        mockMvc.perform(put("/api/tools/{toolId}", toolId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isNoContent());

        verify(toolService).updateTool(eq(toolId), any(UpdateToolInput.class));
    }

    @Test
    @DisplayName("Should delete tool and return 200")
    void testDeleteTool() throws Exception {
        String toolId = UUID.randomUUID().toString();

        mockMvc.perform(delete("/api/tools/{toolId}", toolId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(toolService).deleteTool(toolId);
    }

    // ==================== PRICE FILTERING TESTS ====================

    @Test
    @DisplayName("Should pass minPrice and maxPrice parameters to service")
    void testSearchWithPriceParams() throws Exception {
        // Arrange
        Tool tool = createSampleTool("Budget Drill");
        tool.setPricePerDay(25.0);
        
        when(toolService.searchTools(null, null, 10.0, 50.0)).thenReturn(List.of(tool));

        // Act & Assert
        mockMvc.perform(get("/api/tools/search")
                .param("minPrice", "10.0")
                .param("maxPrice", "50.0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Budget Drill")))
                .andExpect(jsonPath("$[0].pricePerDay", is(25.0)));

        verify(toolService, times(1)).searchTools(null, null, 10.0, 50.0);
    }

    @Test
    @DisplayName("Should handle invalid price type and return 400 Bad Request")
    void testSearchInvalidPriceType() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/tools/search")
                .param("minPrice", "abc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should combine all filters: keyword, location, and price range")
    void testSearchWithAllFilters() throws Exception {
        // Arrange
        Tool tool = createSampleTool("Downtown Drill");
        tool.setPricePerDay(30.0);
        
        when(toolService.searchTools("drill", "Downtown", 10.0, 50.0)).thenReturn(List.of(tool));

        // Act & Assert
        mockMvc.perform(get("/api/tools/search")
                .param("keyword", "drill")
                .param("location", "Downtown")
                .param("minPrice", "10.0")
                .param("maxPrice", "50.0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Downtown Drill")));

        verify(toolService).searchTools("drill", "Downtown", 10.0, 50.0);
    }

    @Test
    @DisplayName("Should handle only minPrice parameter")
    void testSearchWithMinPriceOnly() throws Exception {
        // Arrange
        Tool tool = createSampleTool("Premium Hammer");
        tool.setPricePerDay(100.0);
        
        when(toolService.searchTools(null, null, 50.0, null)).thenReturn(List.of(tool));

        // Act & Assert
        mockMvc.perform(get("/api/tools/search")
                .param("minPrice", "50.0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(toolService).searchTools(null, null, 50.0, null);
    }

    @Test
    @DisplayName("Should handle only maxPrice parameter")
    void testSearchWithMaxPriceOnly() throws Exception {
        // Arrange
        Tool tool = createSampleTool("Budget Saw");
        tool.setPricePerDay(15.0);
        
        when(toolService.searchTools(null, null, null, 20.0)).thenReturn(List.of(tool));

        // Act & Assert
        mockMvc.perform(get("/api/tools/search")
                .param("maxPrice", "20.0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(toolService).searchTools(null, null, null, 20.0);
    }
}
