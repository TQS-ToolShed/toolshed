package com.toolshed.backend.boundary;


import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.service.ToolService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Tools", description = "Operations related to tool listings and search")
@RestController
@RequestMapping("/api/tools")
@CrossOrigin(origins = "*") // Allow frontend access (CORS)
public class ToolController {

    private final ToolService toolService;

    public ToolController(ToolService toolService) {
        this.toolService = toolService;
    }

    /**
     * Search tools by keyword and optional location.
     * Endpoint: GET /api/tools/search?keyword=something&location=somewhere
     */
    @Operation(
        summary = "Search active tools by keyword in title or description and optional location",
        description = "Performs a case-insensitive search across tool titles and descriptions. Filters out inactive tools. Optionally filters by location.",
        responses = {
            @ApiResponse(
                responseCode = "200", 
                description = "Successfully retrieved list of tools",
                content = @Content(mediaType = "application/json", 
                                   schema = @Schema(implementation = Tool.class))
            ),
            @ApiResponse(
                responseCode = "400", 
                description = "Invalid input or missing required parameters"
            )
        }
    )
    @GetMapping("/search")
    public ResponseEntity<List<Tool>> searchTools(
            @Parameter(description = "Keyword to search for (e.g., 'drill', 'saw'). Optional; if omitted returns empty list.")
            @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "Location filter (partial match, case-insensitive). Optional.")
            @RequestParam(value = "location", required = false) String location) {

        List<Tool> results = toolService.searchTools(keyword, location);
        return ResponseEntity.ok(results);
    }
}