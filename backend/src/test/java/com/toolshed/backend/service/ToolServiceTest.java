package com.toolshed.backend.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import org.mockito.junit.jupiter.MockitoExtension;

import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.dto.CreateToolInput;
import com.toolshed.backend.dto.UpdateToolInput;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class) // Initializes mocks
class ToolServiceTest {

    @Mock 
    private ToolRepository toolRepo;

    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private ToolServiceImpl toolService;

    private Tool sampleTool;
    private User supplier;

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

        supplier = new User();
        supplier.setId(UUID.randomUUID());
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

    @Test
    @DisplayName("Should return active tools via service")
    void testGetActiveTools() {
        when(toolRepo.findByActiveTrue()).thenReturn(List.of(sampleTool));

        List<Tool> result = toolService.getActive();

        assertThat(result).containsExactly(sampleTool);
        verify(toolRepo).findByActiveTrue();
    }

    @Test
    @DisplayName("Should create tool with defaults and link to supplier")
    void testCreateTool() {
        when(userRepo.findById(supplier.getId())).thenReturn(Optional.of(supplier));
        when(toolRepo.save(any(Tool.class))).thenAnswer(invocation -> {
            Tool t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        CreateToolInput input = CreateToolInput.builder()
                .title("Saw")
                .description("Sharp saw")
                .pricePerDay(12.0)
                .location("Lisbon")
                .supplierId(supplier.getId())
                .build();

        String newId = toolService.createTool(input);

        assertThat(newId).isNotBlank();
        ArgumentCaptor<Tool> captor = ArgumentCaptor.forClass(Tool.class);
        verify(toolRepo).save(captor.capture());
        Tool saved = captor.getValue();
        assertThat(saved.getOwner()).isEqualTo(supplier);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getOverallRating()).isZero();
        assertThat(saved.getNumRatings()).isZero();
    }

    @Test
    @DisplayName("Should throw when supplier not found during create")
    void testCreateToolSupplierMissing() {
        UUID missingId = UUID.randomUUID();
        CreateToolInput input = CreateToolInput.builder()
                .title("Saw")
                .description("Sharp saw")
                .pricePerDay(12.0)
                .location("Lisbon")
                .supplierId(missingId)
                .build();

        assertThatThrownBy(() -> toolService.createTool(input))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should delete tool when it exists")
    void testDeleteTool() {
        when(toolRepo.existsById(sampleTool.getId())).thenReturn(true);

        toolService.deleteTool(sampleTool.getId().toString());

        verify(toolRepo).deleteById(sampleTool.getId());
    }

    @Test
    @DisplayName("Should throw when deleting missing tool")
    void testDeleteToolMissing() {
        UUID missing = UUID.randomUUID();
        when(toolRepo.existsById(missing)).thenReturn(false);

        assertThatThrownBy(() -> toolService.deleteTool(missing.toString()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should update fields and owner when provided")
    void testUpdateToolWithOwner() {
        UUID newOwnerId = UUID.randomUUID();
        User newOwner = new User();
        newOwner.setId(newOwnerId);

        when(toolRepo.findById(sampleTool.getId())).thenReturn(Optional.of(sampleTool));
        when(userRepo.findById(newOwnerId)).thenReturn(Optional.of(newOwner));

        UpdateToolInput input = UpdateToolInput.builder()
                .title("Updated title")
                .pricePerDay(20.0)
                .ownerId(newOwnerId)
                .active(false)
                .build();

        toolService.updateTool(sampleTool.getId().toString(), input);

        assertThat(sampleTool.getTitle()).isEqualTo("Updated title");
        assertThat(sampleTool.getPricePerDay()).isEqualTo(20.0);
        assertThat(sampleTool.isActive()).isFalse();
        assertThat(sampleTool.getOwner()).isEqualTo(newOwner);
        // unchanged fields preserved
        assertThat(sampleTool.getDescription()).isEqualTo("Desc");
    }

    @Test
    @DisplayName("Should throw when updating missing tool")
    void testUpdateToolMissing() {
        UUID missing = UUID.randomUUID();
        when(toolRepo.findById(missing)).thenReturn(Optional.empty());

        UpdateToolInput input = UpdateToolInput.builder().active(false).build();

        assertThatThrownBy(() -> toolService.updateTool(missing.toString(), input))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should delegate getAll and getById to repository")
    void testGetAllAndGetById() {
        when(toolRepo.findAll()).thenReturn(List.of(sampleTool));
        when(toolRepo.findById(sampleTool.getId())).thenReturn(Optional.of(sampleTool));

        assertThat(toolService.getAll()).containsExactly(sampleTool);
        assertThat(toolService.getById(sampleTool.getId())).contains(sampleTool);
        verify(toolRepo).findAll();
        verify(toolRepo).findById(sampleTool.getId());
    }
}
