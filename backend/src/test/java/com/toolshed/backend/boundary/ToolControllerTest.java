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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.service.ToolService;

@WebMvcTest(ToolController.class) //
class ToolControllerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean 
    private ToolService toolService;

    private Tool createSampleTool(String title) {
        Tool tool = new Tool();
        tool.setId(UUID.randomUUID());
        tool.setTitle(title);
        tool.setDescription("A basic tool");
        tool.setActive(true);
        tool.setPricePerDay(5.0);
        tool.setLocation("Downtown");
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
        when(toolService.searchTools(keyword, location)).thenReturn(List.of(drill));

        // Act & Assert
        mockMvc.perform(get("/api/tools/search")
                .param("keyword", keyword)
                .param("location", location) 
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Downtown Drill")));

        // Verification: Ensure location was NOT null
        verify(toolService, times(1)).searchTools(keyword, location);
    }

    @Test
    @DisplayName("Should return 200 OK and matching tools for a valid search (Location is null)")
    void testSearchTools_ValidKeyword_ReturnsResults() throws Exception {
        // Arrange
        String keyword = "drill";
        Tool drill = createSampleTool("Power Drill");

        // Mock: expect null for location because we don't send the param
        when(toolService.searchTools(keyword, null)).thenReturn(List.of(drill));

        // Act & Assert
        mockMvc.perform(get("/api/tools/search")
                .param("keyword", keyword)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Power Drill")));

        // Verification
        verify(toolService, times(1)).searchTools(keyword, null);
    }

    @Test
    @DisplayName("Should return 200 OK and an empty array when no results are found")
    void testSearchTools_NoMatches_ReturnsEmptyArray() throws Exception {
        // Arrange
        String keyword = "unicorn";

        when(toolService.searchTools(keyword, null)).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/tools/search")
                .param("keyword", keyword)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        // Verification
        verify(toolService, times(1)).searchTools(keyword, null);
    }

    @Test
    @DisplayName("Should handle missing keyword parameter gracefully")
    void testSearchTools_MissingParameter_ReturnsEmptyArray() throws Exception {
        // Arrange
        when(toolService.searchTools(null, null)).thenReturn(Collections.emptyList());

        // Act & Assert
        // Calling endpoint with NO parameters
        mockMvc.perform(get("/api/tools/search")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        // Verification: Ensure service called with nulls
        verify(toolService, times(1)).searchTools(null, null);
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
                .andExpect(jsonPath("$.location", is(tool.getLocation())))
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
}
