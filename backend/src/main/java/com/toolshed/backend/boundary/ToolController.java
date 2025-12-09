package com.toolshed.backend.boundary;


import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.toolshed.backend.dto.CreateToolInput;
import com.toolshed.backend.dto.ToolDetailsResponse;
import com.toolshed.backend.dto.UpdateToolInput;
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
        summary = "Search active tools by keyword and/or location",
        description = "Performs a case-insensitive search across tool titles and descriptions (using 'keyword'). Filters out inactive tools. Optionally filters by location (partial match, case-insensitive). Both parameters are optional; if both are empty, returns all active tools.",
        parameters = {
            @Parameter(name = "keyword", description = "Keyword to search for in title or description (e.g., 'drill'). Optional.", required = false),
            @Parameter(name = "location", description = "Location filter (e.g., 'Aveiro'). Optional.", required = false)
        },
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

    @GetMapping
    public ResponseEntity<List<Tool>> getAllTools() {
        return ResponseEntity.ok(toolService.getAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Tool>> getActiveTools() {
        return ResponseEntity.ok(toolService.getActive());
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<List<Tool>> getToolsBySupplier(@PathVariable String supplierId) {
        UUID id = UUID.fromString(supplierId);
        return ResponseEntity.ok(toolService.getByOwner(id));
    }

    @GetMapping("/{toolId}")
    public ResponseEntity<ToolDetailsResponse> getToolById(@PathVariable String toolId) {
        UUID id = UUID.fromString(toolId);
        return toolService.getById(id)
                .map(this::mapToToolDetails)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Tool>> searchTools(
            @Parameter(description = "Keyword to search for (e.g., 'drill').")
            @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "Filter by location (e.g., 'Aveiro'). Optional.")
            @RequestParam(value = "location", required = false) String location) {
        List<Tool> results = toolService.searchTools(keyword, location);
        return ResponseEntity.ok(results);
    }

    @PostMapping
    public ResponseEntity<Void> createTool(@RequestBody CreateToolInput input) {
        String toolId = toolService.createTool(input);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/tools/{toolId}")
                .buildAndExpand(toolId)
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @PutMapping("/{toolId}")
    public ResponseEntity<Void> updateTool(@PathVariable String toolId, @RequestBody UpdateToolInput input) {
        toolService.updateTool(toolId, input);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{toolId}")
    public ResponseEntity<Void> deleteTool(@PathVariable String toolId) {

        toolService.deleteTool(toolId);
        return ResponseEntity.noContent().build();
    }

    private ToolDetailsResponse mapToToolDetails(Tool tool) {
        return ToolDetailsResponse.builder()
                .id(tool.getId())
                .title(tool.getTitle())
                .description(tool.getDescription())
                .pricePerDay(tool.getPricePerDay())
                .location(tool.getLocation())
                .active(tool.isActive())
                .availabilityCalendar(tool.getAvailabilityCalendar())
                .overallRating(tool.getOverallRating())
                .numRatings(tool.getNumRatings())
                .owner(
                        ToolDetailsResponse.OwnerSummary.builder()
                                .id(tool.getOwner().getId())
                                .firstName(tool.getOwner().getFirstName())
                                .lastName(tool.getOwner().getLastName())
                                .email(tool.getOwner().getEmail())
                                .reputationScore(tool.getOwner().getReputationScore())
                                .build()
                )
                .build();
    }
}
