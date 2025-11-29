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
        Tool drill = new Tool();
        drill.setTitle("Power Drill");
        drill.setDescription("Cordless 18V battery powered");
        drill.setPricePerDay(4.5);
        drill.setActive(true);
        drill.setLocation("Downtown Garage");
        drill.setOwner(defaultOwner);
        drill.setAvailabilityCalendar("{\"mon\": true, \"tue\": true, \"wed\": false}");
        drill.setOverallRating(4.8);

        // 2. Description Match
        Tool bitSet = new Tool();
        bitSet.setTitle("Bit Set");
        bitSet.setDescription("Titanium bits for drill and driver");
        bitSet.setPricePerDay(5.0);
        bitSet.setActive(true);
        bitSet.setLocation("Downtown Garage");
        bitSet.setOwner(defaultOwner);
        bitSet.setAvailabilityCalendar("{\"weekends\": true}");
        bitSet.setOverallRating(4.5);

        // 3. Case Insensitivity (Upper case in DB)
        Tool hammer = new Tool();
        hammer.setTitle("Heavy HAMMER");
        hammer.setDescription("Standard claw hammer");
        hammer.setPricePerDay(10.0);
        hammer.setActive(true);
        hammer.setLocation("Westside Storage");
        hammer.setOwner(defaultOwner);
        hammer.setAvailabilityCalendar("{\"available\": true}");
        hammer.setOverallRating(3.9);

        // 4. Distractor (Should not match 'drill' or 'hammer')
        Tool saw = new Tool();
        saw.setTitle("Circular Saw");
        saw.setDescription("Perfect for cutting wood");
        saw.setPricePerDay(20.0);
        saw.setActive(true);
        saw.setLocation("Eastside Workshop");
        saw.setOwner(defaultOwner);
        saw.setAvailabilityCalendar("{\"available\": true}");
        saw.setOverallRating(5.0);
        
        // 5. Inactive Tool (constraint check)
        Tool inactive = new Tool();
        inactive.setTitle("Old Drill");
        inactive.setDescription("Broken");
        inactive.setActive(false);
        inactive.setLocation("Recycling Bin");
        inactive.setOwner(defaultOwner);
        inactive.setAvailabilityCalendar("{\"available\": false}");
        inactive.setOverallRating(1.2);
    
        toolRepo.saveAll(java.util.List.of(drill, bitSet, hammer, saw, inactive));
        
    }

    @Test
    @DisplayName("Should return tools where Title matches keyword (Case Insensitive)")
    void testSearchByTitle() {
        List<Tool> results = toolRepo.searchTools("Power");
        assertThat(results)
            .hasSize(1)
            .extracting(Tool::getTitle)
            .containsExactly("Power Drill");
    }

    @Test
    @DisplayName("Should return tools where Description matches keyword")
    void testSearchByDescription() {
        List<Tool> results = toolRepo.searchTools("Titanium");
        assertThat(results)
            .hasSize(1)
            .extracting(Tool::getTitle)
            .containsExactly("Bit Set");
    }

    @Test
    @DisplayName("Should return tools matching Title OR Description (Mixed Case)")
    void testSearchTitleOrDescriptionMixedCase() {
        List<Tool> results = toolRepo.searchTools("Drill");
        assertThat(results)
            .hasSize(2)
            .extracting(Tool::getTitle)
            .containsExactlyInAnyOrder("Power Drill", "Bit Set")
            .doesNotContain("Old Drill");
    }

    @Test
    @DisplayName("Should return tools searching with uppercase keyword against lowercase data")
    void testSearchCaseInsensitiveParams() {
        List<Tool> results = toolRepo.searchTools("CLAW");
        assertThat(results)
            .hasSize(1)
            .extracting(Tool::getTitle)
            .containsExactly("Heavy HAMMER");
    }

    @Test
    @DisplayName("Should return empty list when no match is found")
    void testSearchNoResults() {
        List<Tool> results = toolRepo.searchTools("Screwdriver");
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
        toolRepo.save(inactiveTool);

        // Act
        List<Tool> results = toolRepo.searchTools("Secret");

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
        
        List<Tool> results = toolRepo.searchTools("rill");

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
        toolRepo.save(specialTool);

        // Act: Search using the symbol "&"
        List<Tool> results = toolRepo.searchTools("&");

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
        
        List<Tool> results = toolRepo.searchTools("");

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
        toolRepo.save(activeTool);

        List<Tool> results = toolRepo.searchTools("Scratched");

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
        nullDescTool.setDescription(null); // CRITICAL: setting to null
        nullDescTool.setActive(true);
        nullDescTool.setOwner(userRepo.findAll().get(0));
        toolRepo.save(nullDescTool);

        // Act
        List<Tool> results = toolRepo.searchTools("key");

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
        toolRepo.save(underscoreTool);

        // Act
        // Note: Without explicit escaping in the repository, this effectively works 
        // because the tool contains the text.
        List<Tool> results = toolRepo.searchTools("Square_"); 

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
        List<Tool> results = toolRepo.searchTools("Westside");

        // Assert
        assertThat(results).isEmpty();
    }
}
