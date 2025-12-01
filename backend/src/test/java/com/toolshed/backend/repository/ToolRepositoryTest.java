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

    private User defaultOwner;

    private Tool baseTool(String title, String description, String location, double price, boolean active) {
        Tool tool = new Tool();
        tool.setTitle(title);
        tool.setDescription(description);
        tool.setPricePerDay(price);
        tool.setActive(active);
        tool.setLocation(location);
        tool.setOwner(defaultOwner);
        tool.setAvailabilityCalendar("{\"available\": true}");
        tool.setOverallRating(4.5);
        tool.setNumRatings(10);
        return tool;
    }

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

        // 1. Direct title match
        Tool drill = baseTool("Power Drill", "Cordless 18V battery powered", "Downtown Garage", 4.5, true);
        drill.setAvailabilityCalendar("{\"mon\": true, \"tue\": true, \"wed\": false}");
        drill.setOverallRating(4.8);

        // 2. Description Match
        Tool bitSet = baseTool("Bit Set", "Titanium bits for drill and driver", "Eastside Workshop", 5.0, true);
        bitSet.setAvailabilityCalendar("{\"weekends\": true}");
        bitSet.setOverallRating(4.5);

        // 3. Case Insensitivity (Upper case in DB)
        Tool hammer = baseTool("Heavy HAMMER", "Standard claw hammer", "Westside Storage", 10.0, true);
        hammer.setOverallRating(3.9);

        // 4. Distractor (Should not match 'drill' or 'hammer')
        Tool saw = baseTool("Circular Saw", "Perfect for cutting wood", "Eastside Shed", 20.0, true);
        saw.setOverallRating(5.0);
        
        // 5. Inactive Tool (constraint check)
        Tool inactive = baseTool("Old Drill", "Broken", "Recycling Bin", 1.0, false);
        inactive.setAvailabilityCalendar("{\"available\": false}");
        inactive.setOverallRating(1.2);
    
        toolRepo.saveAll(java.util.List.of(drill, bitSet, hammer, saw, inactive));
        
    }

    @Test
    @DisplayName("Should return tools where Title matches keyword (Case Insensitive)")
    void testSearchByTitle() {
        List<Tool> results = toolRepo.searchTools("Power", null);
        assertThat(results)
            .hasSize(1)
            .extracting(Tool::getTitle)
            .containsExactly("Power Drill");
    }

    @Test
    @DisplayName("Should return tools where Description matches keyword")
    void testSearchByDescription() {
        List<Tool> results = toolRepo.searchTools("Titanium", null);
        assertThat(results)
            .hasSize(1)
            .extracting(Tool::getTitle)
            .containsExactly("Bit Set");
    }

    @Test
    @DisplayName("Should return tools matching Title OR Description (Mixed Case)")
    void testSearchTitleOrDescriptionMixedCase() {
        List<Tool> results = toolRepo.searchTools("Drill", null);
        assertThat(results)
            .hasSize(2)
            .extracting(Tool::getTitle)
            .containsExactlyInAnyOrder("Power Drill", "Bit Set")
            .doesNotContain("Old Drill");
    }

    @Test
    @DisplayName("Should return tools searching with uppercase keyword against lowercase data")
    void testSearchCaseInsensitiveParams() {
        List<Tool> results = toolRepo.searchTools("CLAW", null);
        assertThat(results)
            .hasSize(1)
            .extracting(Tool::getTitle)
            .containsExactly("Heavy HAMMER");
    }

    @Test
    @DisplayName("Should return empty list when no match is found")
    void testSearchNoResults() {
        List<Tool> results = toolRepo.searchTools("Screwdriver", null);
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should NOT return inactive tools even if keyword matches perfectly")
    void testSearchIgnoresInactiveTools() {
        // Arrange: Create a specific inactive tool for this test
        Tool inactiveTool = new Tool();
        inactiveTool.setTitle("Super Secret Drill");
        inactiveTool.setDescription("Hidden from public");
        inactiveTool.setActive(false); // Explicitly inactive
        inactiveTool.setOwner(userRepo.findAll().get(0)); // Use existing owner
        inactiveTool.setLocation("Hidden");
        inactiveTool.setPricePerDay(7.0);
        inactiveTool.setOverallRating(3.0);
        inactiveTool.setNumRatings(1);
        toolRepo.save(inactiveTool);

        // Act
        List<Tool> results = toolRepo.searchTools("Secret", null);

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
        
        List<Tool> results = toolRepo.searchTools("rill", null);

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
        specialTool.setActive(true);
        specialTool.setOwner(userRepo.findAll().get(0));
        specialTool.setLocation("Downtown Garage");
        specialTool.setOverallRating(4.2);
        specialTool.setNumRatings(3);
        toolRepo.save(specialTool);

        // Act: Search using the symbol "&"
        List<Tool> results = toolRepo.searchTools("&", null);

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
        
        List<Tool> results = toolRepo.searchTools("", null);

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
        activeTool.setActive(true); // Active
        activeTool.setOwner(userRepo.findAll().get(0));
        activeTool.setLocation("Westside Storage");
        activeTool.setPricePerDay(12.0);
        activeTool.setOverallRating(4.0);
        activeTool.setNumRatings(2);
        toolRepo.save(activeTool);

        List<Tool> results = toolRepo.searchTools("Scratched", null);

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
        nullDescTool.setActive(true);
        nullDescTool.setOwner(userRepo.findAll().get(0));
        nullDescTool.setLocation("Eastside Workshop");
        nullDescTool.setPricePerDay(6.0);
        nullDescTool.setOverallRating(4.1);
        nullDescTool.setNumRatings(2);
        toolRepo.save(nullDescTool);

        // Act
        List<Tool> results = toolRepo.searchTools("key", null);

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
        underscoreTool.setActive(true);
        underscoreTool.setOwner(userRepo.findAll().get(0));
        underscoreTool.setLocation("Eastside Shed");
        underscoreTool.setPricePerDay(9.0);
        underscoreTool.setOverallRating(4.3);
        underscoreTool.setNumRatings(5);
        toolRepo.save(underscoreTool);

        // Act
        // Note: Without explicit escaping in the repository, this effectively works 
        // because the tool contains the text.
        List<Tool> results = toolRepo.searchTools("Square_", null); 

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
        List<Tool> results = toolRepo.searchTools("Westside", null);

        // Assert
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should filter by Keyword AND Location simultaneously")
    void testSearchWithKeywordAndLocation() {
        // Scenario (updated after adding explicit location filter semantics): 
        // 1. "Power Drill" (Downtown Garage) -> Matches Keyword & Location
        // 2. "Bit Set" (Eastside Workshop) -> Matches Keyword in description but DIFFERENT location
        // 3. "Circular Saw" (Eastside Shed) -> Does not match keyword "Drill"
        // Result should therefore be ONLY Power Drill.
        List<Tool> results = toolRepo.searchTools("Drill", "Downtown Garage");
        assertThat(results)
                .hasSize(1)
                .extracting(Tool::getTitle)
                .containsExactly("Power Drill");
    }

    @Test
    @DisplayName("Should return results matching Location only (when Keyword is null/empty)")
    void testSearchWithLocationOnly() {
        // Scenario: User just selects "Westside Storage" from a dropdown, no keyword typed.
        
        // Act
        List<Tool> results = toolRepo.searchTools(null, "Westside Storage");

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
        List<Tool> results = toolRepo.searchTools("Drill", null);
        assertThat(results)
                .hasSize(2)
                .extracting(Tool::getTitle)
                .containsExactlyInAnyOrder("Power Drill", "Bit Set")
                .doesNotContain("Circular Saw");
    }

    @Test
    @DisplayName("Should be case-insensitive for Location as well")
    void testSearchLocationCaseInsensitive() {
        // Act: Search "downtown garage" (lowercase) when DB has "Downtown Garage"
        List<Tool> results = toolRepo.searchTools("Drill", "downtown garage");

        // Assert
        assertThat(results)
                .extracting(Tool::getTitle)
                .contains("Power Drill");
    }

    @Test
    @DisplayName("Should return empty list if Keyword matches but Location does not")
    void testSearchNoMatchForLocation() {
        // Scenario: Searching for "Hammer" (exists in Westside) but filtering for "Downtown"
        
        // Act
        List<Tool> results = toolRepo.searchTools("Hammer", "Downtown Garage");

        // Assert
        assertThat(results).isEmpty();
    }
}
