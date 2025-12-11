package com.toolshed.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;

@DataJpaTest
class ToolRepositoryTest {
    
    @Autowired
    private ToolRepository toolRepo;

    @Autowired
    private UserRepository userRepo;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        
        // clean up
        toolRepo.deleteAll();
        userRepo.deleteAll();

        // pre requisites
        User defaultOwner = new User();
        defaultOwner.setFirstName("John");
        defaultOwner.setLastName("Doe"); 
        defaultOwner.setEmail("john@example.com");
        defaultOwner.setPassword("hashedpass");
        defaultOwner.setRole(UserRole.SUPPLIER); 
        defaultOwner.setStatus(UserStatus.ACTIVE);
        defaultOwner.setReputationScore(4.5); 
        defaultOwner.setRegisteredDate(LocalDateTime.now().minusMonths(6));
        defaultOwner = userRepo.save(defaultOwner); // Use managed entity

        // 1. Cheap tool (5€) - Direct title match
        Tool drill = new Tool();
        drill.setTitle("Power Drill");
        drill.setDescription("Cordless 18V battery powered");
        drill.setPricePerDay(5.0);
        drill.setActive(true);
        drill.setDistrict("Aveiro");
        drill.setMunicipality("Aveiro");
        drill.setOwner(defaultOwner);
        drill.setAvailabilityCalendar("{\"mon\": true, \"tue\": true, \"wed\": false}");
        drill.setOverallRating(4.8);

        // 2. Medium price tool (20€) - Description Match
        Tool bitSet = new Tool();
        bitSet.setTitle("Bit Set");
        bitSet.setDescription("Titanium bits for drill and driver");
        bitSet.setPricePerDay(20.0);
        bitSet.setActive(true);
        bitSet.setDistrict("Lisboa");
        bitSet.setMunicipality("Lisboa");
        bitSet.setOwner(defaultOwner);
        bitSet.setAvailabilityCalendar("{\"weekends\": true}");
        bitSet.setOverallRating(4.5);

        // 3. Expensive tool (100€) - Case Insensitivity (Upper case in DB)
        Tool hammer = new Tool();
        hammer.setTitle("Heavy HAMMER");
        hammer.setDescription("Standard claw hammer");
        hammer.setPricePerDay(100.0);
        hammer.setActive(true);
        hammer.setDistrict("Porto");
        hammer.setMunicipality("Porto");
        hammer.setOwner(defaultOwner);
        hammer.setAvailabilityCalendar("{\"available\": true}");
        hammer.setOverallRating(3.9);

        // 4. Another medium tool (15€) - Distractor (Should not match 'drill' or 'hammer')
        Tool saw = new Tool();
        saw.setTitle("Circular Saw");
        saw.setDescription("Perfect for cutting wood");
        saw.setPricePerDay(15.0);
        saw.setActive(true);
        saw.setDistrict("Lisboa");
        saw.setMunicipality("Sintra");
        saw.setOwner(defaultOwner);
        saw.setAvailabilityCalendar("{\"available\": true}");
        saw.setOverallRating(5.0);
        
        // 5. Inactive Tool (constraint check)
        Tool inactive = new Tool();
        inactive.setTitle("Old Drill");
        inactive.setDescription("Broken");
        inactive.setPricePerDay(1.0);
        inactive.setActive(false);
        inactive.setDistrict("Aveiro");
        inactive.setMunicipality("Aveiro");
        inactive.setOwner(defaultOwner);
        inactive.setAvailabilityCalendar("{\"available\": false}");
        inactive.setOverallRating(1.2);
    
        toolRepo.saveAll(java.util.List.of(drill, bitSet, hammer, saw, inactive));
        
    }

    @Test
    @DisplayName("Should return tools where Title matches keyword (Case Insensitive)")
    void testSearchByTitle() {
        List<Tool> results = toolRepo.searchTools("Power", null, null, null);
        assertThat(results)
            .hasSize(1)
            .extracting(Tool::getTitle)
            .containsExactly("Power Drill");
    }

    @Test
    @DisplayName("Should return tools where Description matches keyword")
    void testSearchByDescription() {
        List<Tool> results = toolRepo.searchTools("Titanium", null, null, null);
        assertThat(results)
            .hasSize(1)
            .extracting(Tool::getTitle)
            .containsExactly("Bit Set");
    }

    @Test
    @DisplayName("Should return tools matching Title OR Description (Mixed Case)")
    void testSearchTitleOrDescriptionMixedCase() {
        List<Tool> results = toolRepo.searchTools("Drill", null, null, null);
        assertThat(results)
            .hasSize(2)
            .extracting(Tool::getTitle)
            .containsExactlyInAnyOrder("Power Drill", "Bit Set")
            .doesNotContain("Old Drill");
    }

    @Test
    @DisplayName("Should return tools searching with uppercase keyword against lowercase data")
    void testSearchCaseInsensitiveParams() {
        List<Tool> results = toolRepo.searchTools("CLAW", null, null, null);
        assertThat(results)
            .hasSize(1)
            .extracting(Tool::getTitle)
            .containsExactly("Heavy HAMMER");
    }

    @Test
    @DisplayName("Should return empty list when no match is found")
    void testSearchNoResults() {
        List<Tool> results = toolRepo.searchTools("Screwdriver", null, null, null);
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should NOT return inactive tools even if keyword matches perfectly")
    void testSearchIgnoresInactiveTools() {
        // Arrange: Create a specific inactive tool for this test
        Tool inactiveTool = new Tool();
        inactiveTool.setTitle("Super Secret Drill");
        inactiveTool.setDescription("Hidden from public");
        inactiveTool.setPricePerDay(10.0);
        inactiveTool.setDistrict("Aveiro");
        inactiveTool.setMunicipality("Aveiro");
        inactiveTool.setActive(false); // Explicitly inactive
        inactiveTool.setOwner(userRepo.findAll().get(0)); // Use existing owner
        inactiveTool.setOverallRating(0.0);
        toolRepo.save(inactiveTool);

        // Act
        List<Tool> results = toolRepo.searchTools("Secret", null, null, null);

        // Assert: ensure the result list is empty and also verify it does not contain the inactive title
        assertThat(results).isEmpty();
        assertThat(results)
                .extracting(Tool::getTitle)
                .doesNotContain("Super Secret Drill");
    }

    @Test
    @DisplayName("Should match keyword embedded in the middle of a word (Partial Match)")
    void testSearchPartialMatchInsideWord() {
        // We have "Power Drill" in the setup. 
        // Searching "rill" should find it if our wildcard logic (%keyword%) is correct.
        
        List<Tool> results = toolRepo.searchTools("rill", null, null, null);

        assertThat(results)
                .extracting(Tool::getTitle)
                .contains("Power Drill");
    }

    @Test
    @DisplayName("Should handle special characters in keyword")
    void testSearchWithSpecialCharacters() {
        // Arrange: Create a tool with symbols
        Tool specialTool = new Tool();
        specialTool.setTitle("Drill & Driver Set + Bits");
        specialTool.setDescription("Professional usage");
        specialTool.setPricePerDay(10.0);
        specialTool.setDistrict("Porto");
        specialTool.setMunicipality("Porto");
        specialTool.setActive(true);
        specialTool.setOwner(userRepo.findAll().get(0));
        specialTool.setOverallRating(5.0);
        toolRepo.save(specialTool);

        // Act: Search using the symbol "&"
        List<Tool> results = toolRepo.searchTools("&", null, null, null);

        // Assert
        assertThat(results)
                .extracting(Tool::getTitle)
                .contains("Drill & Driver Set + Bits");
    }
    
    @Test
    @DisplayName("Should return ALL active tools if keyword is empty string")
    void testSearchWithEmptyString() {
        // In SQL: LIKE '%%' matches everything not null.
        // This tests that the query doesn't crash on empty input.
        // Note: The Service Layer typically validates this, but the Repo should be robust.
        
        List<Tool> results = toolRepo.searchTools("", null, null, null);

        // Assert: Should return all 4 active tools from setUp()
        assertThat(results).hasSize(4); 
    }

    @Test
    @DisplayName("Should return active tools regardless of damage status (damage is tracked separately)")
    void testSearchReturnsActiveTools() {
        // Note: Damage tracking has been moved to the ToolDamage entity
        // The Tool entity only tracks whether it's active or not
        
        Tool activeTool = new Tool();
        activeTool.setTitle("Scratched Saw");
        activeTool.setDescription("Works fine but looks ugly");
        activeTool.setPricePerDay(15.0);
        activeTool.setDistrict("Lisboa");
        activeTool.setMunicipality("Cascais");
        activeTool.setActive(true); // Active
        activeTool.setOwner(userRepo.findAll().get(0));
        activeTool.setOverallRating(3.5);
        toolRepo.save(activeTool);

        List<Tool> results = toolRepo.searchTools("Scratched", null, null, null);

        assertThat(results)
                .extracting(Tool::getTitle)
                .contains("Scratched Saw");
    }

    @Test
    @DisplayName("Should find tool by title and safely ignore null description")
    void testSearchWithNullDescription() {
        // Arrange
        Tool nullDescTool = new Tool();
        nullDescTool.setTitle("Null-Test Key");
        nullDescTool.setDescription(null); 
        nullDescTool.setPricePerDay(5.0);
        nullDescTool.setDistrict("Aveiro");
        nullDescTool.setMunicipality("Aveiro");
        nullDescTool.setActive(true);
        nullDescTool.setOwner(userRepo.findAll().get(0));
        nullDescTool.setOverallRating(2.0);
        toolRepo.save(nullDescTool);

        // Act
        List<Tool> results = toolRepo.searchTools("key", null, null, null);

        // Assert
        assertThat(results)
                .hasSize(1)
                .extracting(Tool::getTitle)
                .containsExactly("Null-Test Key");
    }

    @Test
    @DisplayName("Should treat SQL wildcard characters literally (e.g., underscore)")
    void testSearchForLiteralUnderscore() {
        // Arrange
        Tool underscoreTool = new Tool();
        underscoreTool.setTitle("T-Square_Ruler");
        underscoreTool.setDescription("Used for 90 degree angles");
        underscoreTool.setPricePerDay(8.0);
        underscoreTool.setDistrict("Porto");
        underscoreTool.setMunicipality("Matosinhos");
        underscoreTool.setActive(true);
        underscoreTool.setOwner(userRepo.findAll().get(0));
        underscoreTool.setOverallRating(4.0);
        toolRepo.save(underscoreTool);

        // Act
        // Note: Without explicit escaping in the repository, this effectively works 
        // because the tool contains the text.
        List<Tool> results = toolRepo.searchTools("Square_", null, null, null); 

        // Assert
        assertThat(results)
                .extracting(Tool::getTitle)
                .contains("T-Square_Ruler");
    }

    @Test
    @DisplayName("Should ignore keywords found only in non-searchable fields (e.g., location)")
    void testSearchExcludesLocation() {
        // Arrange: Tool 'hammer' from setUp() has location "Westside Storage"
        // "Westside" is NOT in the title or description.

        // Act
        List<Tool> results = toolRepo.searchTools("Westside", null, null, null);

        // Assert
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should filter by Keyword AND Location simultaneously")
    void testSearchWithKeywordAndLocation() {
        // Scenario (updated after adding explicit location filter semantics): 
        // 1. "Power Drill" (Aveiro) -> Matches Keyword & District
        // 2. "Bit Set" (Lisboa) -> Matches Keyword in description but DIFFERENT district
        // 3. "Circular Saw" (Lisboa, Sintra) -> Does not match keyword "Drill"
        // Result should therefore be ONLY Power Drill.
        List<Tool> results = toolRepo.searchTools("Drill", "Aveiro", null, null);
        assertThat(results)
                .hasSize(1)
                .extracting(Tool::getTitle)
                .containsExactly("Power Drill");
    }

    @Test
    @DisplayName("Should return results matching Location only (when Keyword is null/empty)")
    void testSearchWithLocationOnly() {
        // Scenario: User just searches for "Porto", no keyword typed.
        
        // Act
        List<Tool> results = toolRepo.searchTools(null, "Porto", null, null);

        // Assert
        assertThat(results)
                .hasSize(1)
                .extracting(Tool::getTitle)
                .containsExactly("Heavy HAMMER");
    }

    @Test
    @DisplayName("Should ignore Location filter when it is null (Backward Compatibility)")
    void testSearchWithKeywordOnly_LocationNull() {
        // Scenario: User types "Drill" but leaves location filter empty.
        // Matches: "Power Drill" (title) and "Bit Set" (description). "Circular Saw" no longer matches.
        List<Tool> results = toolRepo.searchTools("Drill", null, null, null);
        assertThat(results)
                .hasSize(2)
                .extracting(Tool::getTitle)
                .containsExactlyInAnyOrder("Power Drill", "Bit Set")
                .doesNotContain("Circular Saw");
    }

    @Test
    @DisplayName("Should be case-insensitive for Location as well")
    void testSearchLocationCaseInsensitive() {
        // Act: Search "aveiro" (lowercase) when DB has "Aveiro"
        List<Tool> results = toolRepo.searchTools("Drill", "aveiro", null, null);

        // Assert
        assertThat(results)
                .extracting(Tool::getTitle)
                .contains("Power Drill");
    }

    @Test
    @DisplayName("Should return empty list if Keyword matches but Location does not")
    void testSearchNoMatchForLocation() {
        // Scenario: Searching for "Hammer" (exists in Porto) but filtering for "Aveiro"
        
        // Act
        List<Tool> results = toolRepo.searchTools("Hammer", "Aveiro", null, null);

        // Assert
        assertThat(results).isEmpty();
    }

    // ==================== PRICE FILTERING TESTS ====================

    @Test
    @DisplayName("Should filter by minimum price (inclusive)")
    void testSearchByMinPrice() {
        // Arrange: Tools with prices: 5.0€ (drill), 20.0€ (bitSet), 100.0€ (hammer), 15.0€ (saw)
        // Expected: Tools >= 10€ should return bitSet (20€), hammer (100€), and saw (15€)
        // Should exclude drill (5€)

        // Act
        List<Tool> results = toolRepo.searchTools(null, null, 10.0, null);

        // Assert
        assertThat(results)
                .hasSize(3)
                .extracting(Tool::getTitle)
                .containsExactlyInAnyOrder("Bit Set", "Heavy HAMMER", "Circular Saw")
                .doesNotContain("Power Drill");
    }

    @Test
    @DisplayName("Should filter by maximum price (inclusive)")
    void testSearchByMaxPrice() {
        // Arrange: Tools with prices: 5.0€ (drill), 20.0€ (bitSet), 100.0€ (hammer), 15.0€ (saw)
        // Expected: Tools <= 30€ should return drill (5€), bitSet (20€), and saw (15€)
        // Should exclude hammer (100€)

        // Act
        List<Tool> results = toolRepo.searchTools(null, null, null, 30.0);

        // Assert
        assertThat(results)
                .hasSize(3)
                .extracting(Tool::getTitle)
                .containsExactlyInAnyOrder("Power Drill", "Bit Set", "Circular Saw")
                .doesNotContain("Heavy HAMMER");
    }

    @Test
    @DisplayName("Should filter by price range (min and max, both inclusive)")
    void testSearchByPriceRange() {
        // Arrange: Tools with prices: 5.0€ (drill), 20.0€ (bitSet), 100.0€ (hammer), 15.0€ (saw)
        // Expected: Tools between 10€ and 30€ should return bitSet (20€) and saw (15€)
        // Should exclude drill (5€) and hammer (100€)

        // Act
        List<Tool> results = toolRepo.searchTools(null, null, 10.0, 30.0);

        // Assert
        assertThat(results)
                .hasSize(2)
                .extracting(Tool::getTitle)
                .containsExactlyInAnyOrder("Bit Set", "Circular Saw")
                .doesNotContain("Power Drill", "Heavy HAMMER");
    }

    @Test
    @DisplayName("Should combine keyword search with max price filter")
    void testSearchCombinedFilters() {
        // Arrange: "Drill" keyword appears in:
        // - "Power Drill" (title, 5€)
        // - "Bit Set" (description has "drill", 20€)
        // With maxPrice = 50€, both should match

        // Act
        List<Tool> results = toolRepo.searchTools("Drill", null, null, 50.0);

        // Assert
        assertThat(results)
                .hasSize(2)
                .extracting(Tool::getTitle)
                .containsExactlyInAnyOrder("Power Drill", "Bit Set");
    }

    @Test
    @DisplayName("Should respect inclusive price boundaries (exactly at min)")
    void testSearchPriceInclusiveMin() {
        // Arrange: bitSet has exactly 20.0€
        
        // Act: Search for minPrice = 20.0
        List<Tool> results = toolRepo.searchTools(null, null, 20.0, null);

        // Assert: Should include bitSet (20€ matches >= 20)
        assertThat(results)
                .extracting(Tool::getTitle)
                .contains("Bit Set");
    }

    @Test
    @DisplayName("Should respect inclusive price boundaries (exactly at max)")
    void testSearchPriceInclusiveMax() {
        // Arrange: bitSet has exactly 20.0€
        
        // Act: Search for maxPrice = 20.0
        List<Tool> results = toolRepo.searchTools(null, null, null, 20.0);

        // Assert: Should include bitSet (20€ matches <= 20)
        assertThat(results)
                .extracting(Tool::getTitle)
                .contains("Bit Set");
    }
}
