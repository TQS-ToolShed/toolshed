package com.toolshed.backend.service;

import java.time.LocalDate;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.toolshed.backend.dto.CreateToolInput;
import com.toolshed.backend.dto.UpdateToolInput;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;

@ExtendWith(MockitoExtension.class) // Initializes mocks
class ToolServiceTest {

    @Mock
    private ToolRepository toolRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private BookingRepository bookingRepo;

    @Mock
    private IGeoApiService geoApiService;

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
        sampleTool.setDistrict("Aveiro");
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
        when(toolRepo.searchTools(keyword, null, null, null)).thenReturn(List.of(sampleTool));

        // Act
        List<Tool> result = toolService.searchTools(keyword, null, null, null);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Mock Drill");

        // Verification: Did the service actually call the repo?
        verify(toolRepo, times(1)).searchTools(keyword, null, null, null);
    }

    @Test
    @DisplayName("Should trim whitespace from keyword before calling repository")
    void testSearchToolsTrimsWhitespace() {
        // Arrange: User enters " Drill "
        String dirtyKeyword = "  Drill  ";
        String cleanedKeyword = "Drill";

        when(toolRepo.searchTools(cleanedKeyword, null, null, null)).thenReturn(List.of(sampleTool));

        // Act
        toolService.searchTools(dirtyKeyword, null, null, null);

        // Assert
        // Verify the repo was called with the TRIMMED version, not the dirty one
        verify(toolRepo).searchTools(cleanedKeyword, null, null, null);
        verify(toolRepo, never()).searchTools(dirtyKeyword, null, null, null);
    }

    @Test
    @DisplayName("Should return empty list immediately if both keyword and district are null (Defensive Coding)")
    void testSearchToolsWithNullKeywordAndDistrict() {
        // Act
        List<Tool> result = toolService.searchTools(null, null, null, null);

        // Assert
        assertThat(result).isEmpty();

        // Verify we NEVER bothered the database with a null query
        verifyNoInteractions(toolRepo);
    }

    @Test
    @DisplayName("Should search with district only when keyword is null")
    void testSearchToolsWithDistrictOnly() {
        // Arrange
        String district = "Aveiro";
        when(toolRepo.searchTools(null, district, null, null)).thenReturn(List.of(sampleTool));

        // Act
        List<Tool> result = toolService.searchTools(null, district, null, null);

        // Assert
        assertThat(result).hasSize(1);
        verify(toolRepo, times(1)).searchTools(null, district, null, null);
    }

    @Test
    @DisplayName("Should return empty list when repository returns no matches")
    void testSearchToolsNoResults() {
        // Arrange
        String keyword = "Unicorn";
        when(toolRepo.searchTools(keyword, null, null, null)).thenReturn(Collections.emptyList());

        // Act
        List<Tool> result = toolService.searchTools(keyword, null, null, null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should ensure search calls specific query and not generic findAll()")
    void testSearchCallsSpecificMethod() {
        String keyword = "hammer";

        // Arrange
        when(toolRepo.searchTools(keyword, null, null, null)).thenReturn(List.of(sampleTool));

        // Act
        toolService.searchTools(keyword, null, null, null);

        // Assert
        // Verify that the specific method designed for filtering was called
        verify(toolRepo, times(1)).searchTools(keyword, null, null, null);

        // Safety check: Verify that a generic, unfiltered method was NOT called
        verify(toolRepo, never()).findAll();
    }

    @Test
    @DisplayName("Should delegate original casing to repository (relying on JPQL for case-insensitivity)")
    void testSearchPreservesCasing() {
        // Arrange
        String mixedCaseKeyword = "HaMmEr";

        // Act
        toolService.searchTools(mixedCaseKeyword, null, null, null);

        // Assert
        // Verify the repository was called exactly with the mixed-case string
        verify(toolRepo).searchTools(mixedCaseKeyword, null, null, null);

        // Safety check: Verify the service did NOT try to change the keyword to
        // lowercase itself
        verify(toolRepo, never()).searchTools(mixedCaseKeyword.toLowerCase(), null, null, null);
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
        when(geoApiService.districtExists("Aveiro")).thenReturn(true);
        when(toolRepo.save(any(Tool.class))).thenAnswer(invocation -> {
            Tool t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        CreateToolInput input = CreateToolInput.builder()
                .title("Saw")
                .description("Sharp saw")
                .pricePerDay(12.0)
                .district("Aveiro")
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
        assertThat(saved.getDistrict()).isEqualTo("Aveiro");
    }

    @Test
    @DisplayName("Should throw when supplier not found during create")
    void testCreateToolSupplierMissing() {
        UUID missingId = UUID.randomUUID();
        CreateToolInput input = CreateToolInput.builder()
                .title("Saw")
                .description("Sharp saw")
                .pricePerDay(12.0)
                .district("Aveiro")
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

        String missingId = missing.toString();

        assertThatThrownBy(() -> toolService.deleteTool(missingId))
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
    @DisplayName("Should reject activating tool when it is currently rented")
    void testUpdateToolActiveBlockedByRental() {
        sampleTool.setActive(false);
        when(toolRepo.findById(sampleTool.getId())).thenReturn(Optional.of(sampleTool));
        when(bookingRepo.countActiveApprovedBookingsForToolOnDate(eq(sampleTool.getId()), any(LocalDate.class)))
                .thenReturn(2L);

        UpdateToolInput input = UpdateToolInput.builder()
                .active(true)
                .build();

        String toolId = sampleTool.getId().toString();

        assertThatThrownBy(() -> toolService.updateTool(toolId, input))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.CONFLICT);

        // ensure active flag unchanged and no save performed
        assertThat(sampleTool.isActive()).isFalse();
        verify(toolRepo, never()).save(any(Tool.class));
    }

    @Test
    @DisplayName("Should activate tool when there are no active rentals")
    void testUpdateToolActiveWhenNoRentals() {
        sampleTool.setActive(false);
        when(toolRepo.findById(sampleTool.getId())).thenReturn(Optional.of(sampleTool));
        when(bookingRepo.countActiveApprovedBookingsForToolOnDate(eq(sampleTool.getId()), any(LocalDate.class)))
                .thenReturn(0L);

        UpdateToolInput input = UpdateToolInput.builder()
                .active(true)
                .build();

        toolService.updateTool(sampleTool.getId().toString(), input);

        assertThat(sampleTool.isActive()).isTrue();
        verify(toolRepo).save(sampleTool);
    }

    @Test
    @DisplayName("Should update all editable fields when present")
    void testUpdateToolAllFields() {
        UUID newOwnerId = UUID.randomUUID();
        User newOwner = new User();
        newOwner.setId(newOwnerId);

        when(toolRepo.findById(sampleTool.getId())).thenReturn(Optional.of(sampleTool));
        when(userRepo.findById(newOwnerId)).thenReturn(Optional.of(newOwner));
        when(geoApiService.districtExists("Lisboa")).thenReturn(true);

        UpdateToolInput input = UpdateToolInput.builder()
                .title("New Title")
                .description("New Desc")
                .pricePerDay(25.0)
                .district("Lisboa")
                .active(true)
                .availabilityCalendar("cal-json")
                .overallRating(4.9)
                .numRatings(12)
                .ownerId(newOwnerId)
                .build();

        toolService.updateTool(sampleTool.getId().toString(), input);

        assertThat(sampleTool.getTitle()).isEqualTo("New Title");
        assertThat(sampleTool.getDescription()).isEqualTo("New Desc");
        assertThat(sampleTool.getPricePerDay()).isEqualTo(25.0);
        assertThat(sampleTool.getDistrict()).isEqualTo("Lisboa");
        assertThat(sampleTool.isActive()).isTrue();
        assertThat(sampleTool.getAvailabilityCalendar()).isEqualTo("cal-json");
        assertThat(sampleTool.getOverallRating()).isEqualTo(4.9);
        assertThat(sampleTool.getNumRatings()).isEqualTo(12);
        assertThat(sampleTool.getOwner()).isEqualTo(newOwner);
    }

    @Test
    @DisplayName("Should throw when updating missing tool")
    void testUpdateToolMissing() {
        UUID missing = UUID.randomUUID();
        when(toolRepo.findById(missing)).thenReturn(Optional.empty());

        UpdateToolInput input = UpdateToolInput.builder().active(false).build();

        String missingId = missing.toString();

        assertThatThrownBy(() -> toolService.updateTool(missingId, input))
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

    // ==================== PRICE FILTERING TESTS ====================

    @Test
    @DisplayName("Should pass price range to repository when both min and max are provided")
    void testSearchWithPriceRange() {
        // Arrange
        Double minPrice = 10.0;
        Double maxPrice = 50.0;
        when(toolRepo.searchTools(null, null, minPrice, maxPrice)).thenReturn(List.of(sampleTool));

        // Act
        List<Tool> result = toolService.searchTools(null, null, minPrice, maxPrice);

        // Assert
        assertThat(result).hasSize(1);
        verify(toolRepo, times(1)).searchTools(null, null, minPrice, maxPrice);
    }

    @Test
    @DisplayName("Should sanitize negative min price to 0.0")
    void testSearchWithNegativeMinPrice() {
        // Arrange
        Double negativeMinPrice = -5.0;
        when(toolRepo.searchTools(null, null, 0.0, null)).thenReturn(List.of(sampleTool));

        // Act
        toolService.searchTools(null, null, negativeMinPrice, null);

        // Assert
        // Verify the repository was called with 0.0 instead of -5.0
        verify(toolRepo).searchTools(null, null, 0.0, null);
        verify(toolRepo, never()).searchTools(null, null, negativeMinPrice, null);
    }

    @Test
    @DisplayName("Should sanitize negative max price to 0.0")
    void testSearchWithNegativeMaxPrice() {
        // Arrange
        Double negativeMaxPrice = -10.0;
        when(toolRepo.searchTools(null, null, null, 0.0)).thenReturn(Collections.emptyList());

        // Act
        toolService.searchTools(null, null, null, negativeMaxPrice);

        // Assert
        verify(toolRepo).searchTools(null, null, null, 0.0);
        verify(toolRepo, never()).searchTools(null, null, null, negativeMaxPrice);
    }

    @Test
    @DisplayName("Should allow searching with only minPrice")
    void testSearchWithMinPriceOnly() {
        // Arrange
        Double minPrice = 20.0;
        when(toolRepo.searchTools(null, null, minPrice, null)).thenReturn(List.of(sampleTool));

        // Act
        List<Tool> result = toolService.searchTools(null, null, minPrice, null);

        // Assert
        assertThat(result).hasSize(1);
        verify(toolRepo).searchTools(null, null, minPrice, null);
    }

    @Test
    @DisplayName("Should allow searching with only maxPrice")
    void testSearchWithMaxPriceOnly() {
        // Arrange
        Double maxPrice = 100.0;
        when(toolRepo.searchTools(null, null, null, maxPrice)).thenReturn(List.of(sampleTool));

        // Act
        List<Tool> result = toolService.searchTools(null, null, null, maxPrice);

        // Assert
        assertThat(result).hasSize(1);
        verify(toolRepo).searchTools(null, null, null, maxPrice);
    }

    @Test
    @DisplayName("Should combine keyword, district, and price filters")
    void testSearchWithAllFilters() {
        // Arrange
        String keyword = "drill";
        String district = "Porto";
        Double minPrice = 10.0;
        Double maxPrice = 50.0;
        when(toolRepo.searchTools(keyword, district, minPrice, maxPrice)).thenReturn(List.of(sampleTool));

        // Act
        List<Tool> result = toolService.searchTools(keyword, district, minPrice, maxPrice);

        // Assert
        assertThat(result).hasSize(1);
        verify(toolRepo).searchTools(keyword, district, minPrice, maxPrice);
    }

    // ==================== DISTRICT VALIDATION TESTS ====================

    @Test
    @DisplayName("Should throw when creating tool with invalid district")
    void testCreateToolInvalidDistrict() {
        when(userRepo.findById(supplier.getId())).thenReturn(Optional.of(supplier));
        when(geoApiService.districtExists("InvalidDistrict")).thenReturn(false);

        CreateToolInput input = CreateToolInput.builder()
                .title("Saw")
                .description("Sharp saw")
                .pricePerDay(12.0)
                .district("InvalidDistrict")
                .supplierId(supplier.getId())
                .build();

        assertThatThrownBy(() -> toolService.createTool(input))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should throw when updating tool with invalid district")
    void testUpdateToolInvalidDistrict() {
        when(toolRepo.findById(sampleTool.getId())).thenReturn(Optional.of(sampleTool));
        when(geoApiService.districtExists("InvalidDistrict")).thenReturn(false);

        UpdateToolInput input = UpdateToolInput.builder()
                .district("InvalidDistrict")
                .build();

        String toolId = sampleTool.getId().toString();

        assertThatThrownBy(() -> toolService.updateTool(toolId, input))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should return empty list when keyword is empty string")
    void testSearchToolsWithEmptyKeyword() {
        List<Tool> result = toolService.searchTools("   ", "   ", null, null);

        assertThat(result).isEmpty();
        verifyNoInteractions(toolRepo);
    }

    @Test
    @DisplayName("Should search with valid keyword and empty location")
    void testSearchToolsWithKeywordAndEmptyLocation() {
        String keyword = "Drill";
        when(toolRepo.searchTools(keyword, "", null, null)).thenReturn(List.of(sampleTool));

        List<Tool> result = toolService.searchTools(keyword, "   ", null, null);

        assertThat(result).hasSize(1);
        verify(toolRepo).searchTools(keyword, "", null, null);
    }

    @Test
    @DisplayName("Should search with valid location and empty keyword")
    void testSearchToolsWithLocationAndEmptyKeyword() {
        String location = "Aveiro";
        when(toolRepo.searchTools("", location, null, null)).thenReturn(List.of(sampleTool));

        List<Tool> result = toolService.searchTools("   ", location, null, null);

        assertThat(result).hasSize(1);
        verify(toolRepo).searchTools("", location, null, null);
    }

    @Test
    @DisplayName("Should allow setting active to true when tool is already active")
    void testUpdateToolActiveWhenAlreadyActive() {
        sampleTool.setActive(true);
        when(toolRepo.findById(sampleTool.getId())).thenReturn(Optional.of(sampleTool));

        UpdateToolInput input = UpdateToolInput.builder()
                .active(true)
                .build();

        toolService.updateTool(sampleTool.getId().toString(), input);

        assertThat(sampleTool.isActive()).isTrue();
        verify(toolRepo).save(sampleTool);
        // Should not check rental count when tool is already active
        verify(bookingRepo, never()).countActiveApprovedBookingsForToolOnDate(any(UUID.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("Should get tools by owner id")
    void testGetByOwner() {
        sampleTool.setOwner(supplier);
        when(toolRepo.findByOwnerId(supplier.getId())).thenReturn(List.of(sampleTool));

        List<Tool> result = toolService.getByOwner(supplier.getId());

        assertThat(result).containsExactly(sampleTool);
        verify(toolRepo).findByOwnerId(supplier.getId());
    }

    @Test
    @DisplayName("Should create tool with image URL")
    void testCreateToolWithImage() {
        when(userRepo.findById(supplier.getId())).thenReturn(Optional.of(supplier));
        when(geoApiService.districtExists("Aveiro")).thenReturn(true);
        when(toolRepo.save(any(Tool.class))).thenAnswer(invocation -> {
            Tool t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        String imageUrl = "http://example.com/drill.jpg";
        CreateToolInput input = CreateToolInput.builder()
                .title("Saw")
                .description("Sharp saw")
                .pricePerDay(12.0)
                .district("Aveiro")
                .supplierId(supplier.getId())
                .imageUrl(imageUrl)
                .build();

        String newId = toolService.createTool(input);

        assertThat(newId).isNotBlank();
        ArgumentCaptor<Tool> captor = ArgumentCaptor.forClass(Tool.class);
        verify(toolRepo).save(captor.capture());
        Tool saved = captor.getValue();
        assertThat(saved.getImageUrl()).isEqualTo(imageUrl);
    }

    @Test
    @DisplayName("Should set active to false and update maintenance date when maintenance is set")
    void testSetMaintenance_SetsFieldsAndInactive() {
        when(toolRepo.findById(sampleTool.getId())).thenReturn(Optional.of(sampleTool));
        when(toolRepo.save(sampleTool)).thenReturn(sampleTool);

        LocalDate maintenanceDate = LocalDate.now().plusDays(5);
        toolService.setMaintenance(sampleTool.getId().toString(), maintenanceDate);

        assertThat(sampleTool.isUnderMaintenance()).isTrue();
        assertThat(sampleTool.getMaintenanceAvailableDate()).isEqualTo(maintenanceDate);
        assertThat(sampleTool.isActive()).isFalse();
        verify(toolRepo).save(sampleTool);
    }

    @Test
    @DisplayName("Should set active to true and clear maintenance date when maintenance is cleared")
    void testSetMaintenance_ClearsFieldsAndActive() {
        sampleTool.setUnderMaintenance(true);
        sampleTool.setMaintenanceAvailableDate(LocalDate.now().plusDays(5));
        sampleTool.setActive(false);

        when(toolRepo.findById(sampleTool.getId())).thenReturn(Optional.of(sampleTool));
        when(toolRepo.save(sampleTool)).thenReturn(sampleTool);

        toolService.setMaintenance(sampleTool.getId().toString(), null);

        assertThat(sampleTool.isUnderMaintenance()).isFalse();
        assertThat(sampleTool.getMaintenanceAvailableDate()).isNull();
        assertThat(sampleTool.isActive()).isTrue();
        verify(toolRepo).save(sampleTool);
    }
}
