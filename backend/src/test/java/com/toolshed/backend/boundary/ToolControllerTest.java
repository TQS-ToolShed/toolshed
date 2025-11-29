package com.toolshed.backend.boundary;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
import com.toolshed.backend.service.ToolService;

@WebMvcTest(ToolController.class) //
class ToolControllerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean 
    private ToolService toolService;

    private Tool createSampleTool(String title) {
        Tool tool = new Tool();
        tool.setTitle(title);
        tool.setDescription("A basic tool");
        tool.setActive(true);
        tool.setPricePerDay(5.0);
        // NOTE: In a real test, you would set all mandatory fields including the Owner
        return tool;
    }

    @Test
    @DisplayName("Should return 200 OK and matching tools for a valid search")
    void testSearchTools_ValidKeyword_ReturnsResults() throws Exception {
        // Arrange
        String keyword = "drill";
        Tool drill = createSampleTool("Power Drill");
        
        // Mock the Service call
        when(toolService.searchTools(keyword, null)).thenReturn(List.of(drill));

        // Act & Assert
        mockMvc.perform(get("/api/tools/search")
                .param("keyword", keyword)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // HTTP Status 200
                .andExpect(jsonPath("$", hasSize(1))) // Check array size
                .andExpect(jsonPath("$[0].title", is("Power Drill"))); // Check content
        
        // Verification: Ensure the service layer was called correctly
        verify(toolService, times(1)).searchTools(keyword, null);
    }

    @Test
    @DisplayName("Should return 200 OK and an empty array when no results are found")
    void testSearchTools_NoMatches_ReturnsEmptyArray() throws Exception {
        // Arrange
        String keyword = "unicorn";
        
        // Mock the Service call to return an empty list
        when(toolService.searchTools(keyword, null)).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/tools/search")
                .param("keyword", keyword)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // Status 200 OK (empty list is success)
                .andExpect(content().json("[]")); // The response body is an empty JSON array

        // Verification
        verify(toolService, times(1)).searchTools(keyword, null);
    }
    
    @Test
    @DisplayName("Should handle missing keyword parameter gracefully")
    void testSearchTools_MissingParameter_ReturnsEmptyArray() throws Exception {
        // Arrange
        // The service layer should return an empty list when faced with a null/empty query
        when(toolService.searchTools(null, null)).thenReturn(Collections.emptyList());

        // Act & Assert
        // We call the endpoint without the required 'keyword' parameter
        mockMvc.perform(get("/api/tools/search")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // Still 200 OK
                .andExpect(content().json("[]"));

        // Verification: Ensure the service was called with 'null' or handled appropriately
        verify(toolService, times(1)).searchTools(null, null); 
    }
}
