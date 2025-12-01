package com.toolshed.backend.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.dto.UpdateToolInput;
import com.toolshed.backend.repository.entities.Tool;

@ExtendWith(MockitoExtension.class) // Initializes mocks
class ToolServiceTest {

    @Mock 
    private ToolRepository toolRepo;

    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private ToolServiceImpl toolService;

    private Tool sampleTool;

    @BeforeEach
    void setUp() {
        // Create a simple dummy tool for returning in mocks
        sampleTool = new Tool();
        sampleTool.setId(UUID.randomUUID());
        sampleTool.setTitle("Mock Drill");
        sampleTool.setDescription("Desc");
        sampleTool.setLocation("Loc");
        sampleTool.setPricePerDay(10.0);
        sampleTool.setNumRatings(2);
        sampleTool.setOverallRating(4.0);
        sampleTool.setActive(true);
    }

    @Test
    @DisplayName("Should delegate search to repository when keyword is valid")
    void testSearchToolsWithValidKeyword() {
        // Arrange
        String keyword = "Drill";
        when(toolRepo.searchTools(keyword, null)).thenReturn(List.of(sampleTool));

        // Act
        List<Tool> result = toolService.searchTools(keyword, null);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Mock Drill");
        
        // Verification: Did the service actually call the repo?
        verify(toolRepo, times(1)).searchTools(keyword, null);
    }

    @Test
    @DisplayName("Should trim whitespace from keyword before calling repository")
    void testSearchToolsTrimsWhitespace() {
        // Arrange: User enters "  Drill  "
        String dirtyKeyword = "  Drill  ";
        String cleanedKeyword = "Drill";
        
        when(toolRepo.searchTools(cleanedKeyword, null)).thenReturn(List.of(sampleTool));

        // Act
        toolService.searchTools(dirtyKeyword, null);

        // Assert
        // Verify the repo was called with the TRIMMED version, not the dirty one
        verify(toolRepo).searchTools(cleanedKeyword, null);
        verify(toolRepo, never()).searchTools(dirtyKeyword, null);
    }

    @Test
    @DisplayName("Should return empty list immediately if keyword is null (Defensive Coding)")
    void testSearchToolsWithNullKeyword() {
        // Act
        List<Tool> result = toolService.searchTools(null, null);

        // Assert
        assertThat(result).isEmpty();
        
        // Verify we NEVER bothered the database with a null query
        verifyNoInteractions(toolRepo);
    }

    @Test
    @DisplayName("Should return empty list when repository returns no matches")
    void testSearchToolsNoResults() {
        // Arrange
        String keyword = "Unicorn";
        when(toolRepo.searchTools(keyword, null)).thenReturn(Collections.emptyList());

        // Act
        List<Tool> result = toolService.searchTools(keyword, null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should ensure search calls specific query and not generic findAll()")
    void testSearchCallsSpecificMethod() {
        String keyword = "hammer";
        
        // Arrange
        when(toolRepo.searchTools(keyword, null)).thenReturn(List.of(sampleTool));

        // Act
        toolService.searchTools(keyword, null);

        // Assert
        // Verify that the specific method designed for filtering was called
        verify(toolRepo, times(1)).searchTools(keyword, null);
        
        // Safety check: Verify that a generic, unfiltered method was NOT called
        verify(toolRepo, never()).findAll(); 
    }

    @Test
    @DisplayName("Should delegate original casing to repository (relying on JPQL for case-insensitivity)")
    void testSearchPreservesCasing() {
        // Arrange
        String mixedCaseKeyword = "HaMmEr";

        // Act
        toolService.searchTools(mixedCaseKeyword, null);

        // Assert
        // Verify the repository was called exactly with the mixed-case string
        verify(toolRepo).searchTools(mixedCaseKeyword, null);
        
        // Safety check: Verify the service did NOT try to change the keyword to lowercase itself
        verify(toolRepo, never()).searchTools(mixedCaseKeyword.toLowerCase(), null);
    }

    @Test
    @DisplayName("Should toggle only active flag without nulling other fields")
    void testUpdateToolActiveOnly() {
        when(toolRepo.findById(sampleTool.getId())).thenReturn(Optional.of(sampleTool));
        when(toolRepo.save(sampleTool)).thenReturn(sampleTool);

        UpdateToolInput input = UpdateToolInput.builder()
                .active(false)
                .build();

        toolService.updateTool(sampleTool.getId().toString(), input);

        assertThat(sampleTool.isActive()).isFalse();
        assertThat(sampleTool.getNumRatings()).isEqualTo(2);
        assertThat(sampleTool.getOverallRating()).isEqualTo(4.0);
        verify(toolRepo).save(sampleTool);
    }
}
