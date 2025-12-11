package com.toolshed.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.toolshed.backend.dto.CreateToolInput;
import com.toolshed.backend.dto.UpdateToolInput;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ReviewRepository;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ToolIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private User supplier;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        bookingRepository.deleteAll();
        toolRepository.deleteAll();
        userRepository.deleteAll();

        supplier = new User();
        supplier.setFirstName("John");
        supplier.setLastName("Doe");
        supplier.setEmail("john@example.com");
        supplier.setPassword("password");
        supplier.setReputationScore(5.0);
        supplier.setRole(UserRole.SUPPLIER);
        supplier.setStatus(UserStatus.ACTIVE);
        userRepository.save(supplier);
    }

    @Test
    void testCreateAndGetTool() {
        CreateToolInput input = CreateToolInput.builder()
                .title("Integration Drill")
                .description("A drill for integration testing")
                .pricePerDay(15.0)
                .district("Aveiro")
                .municipality("Aveiro")
                .supplierId(supplier.getId())
                .build();

        ResponseEntity<Void> createResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/tools", input, Void.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Verify it exists in DB
        assertThat(toolRepository.count()).isEqualTo(1);
        Tool savedTool = toolRepository.findAll().get(0);
        assertThat(savedTool.getTitle()).isEqualTo("Integration Drill");

        // Get by ID
        ResponseEntity<Tool> getResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/tools/" + savedTool.getId(), Tool.class);
        
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getTitle()).isEqualTo("Integration Drill");
    }

    @Test
    void testSearchTools() {
        Tool tool1 = Tool.builder()
                .title("Hammer")
                .description("Heavy hammer")
                .pricePerDay(5.0)
                .district("Lisboa")
                .municipality("Lisboa")
                .owner(supplier)
                .active(true)
                .overallRating(0.0)
                .numRatings(0)
                .build();
        toolRepository.save(tool1);

        Tool tool2 = Tool.builder()
                .title("Screwdriver")
                .description("Small screwdriver")
                .pricePerDay(2.0)
                .district("Porto")
                .municipality("Porto")
                .owner(supplier)
                .active(true)
                .overallRating(0.0)
                .numRatings(0)
                .build();
        toolRepository.save(tool2);

        ResponseEntity<Tool[]> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/tools/search?keyword=Hammer", Tool[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()[0].getTitle()).isEqualTo("Hammer");
    }

    @Test
    void testUpdateTool() {
        Tool tool = Tool.builder()
                .title("Old Title")
                .description("Old Desc")
                .pricePerDay(10.0)
                .district("Aveiro")
                .municipality("Aveiro")
                .owner(supplier)
                .active(true)
                .overallRating(0.0)
                .numRatings(0)
                .build();
        toolRepository.save(tool);

        UpdateToolInput input = UpdateToolInput.builder()
                .title("New Title")
                .pricePerDay(20.0)
                .build();

        HttpEntity<UpdateToolInput> requestEntity = new HttpEntity<>(input);
        ResponseEntity<Void> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/tools/" + tool.getId(),
                HttpMethod.PUT, requestEntity, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        Tool updatedTool = toolRepository.findById(tool.getId()).orElseThrow();
        assertThat(updatedTool.getTitle()).isEqualTo("New Title");
        assertThat(updatedTool.getPricePerDay()).isEqualTo(20.0);
        assertThat(updatedTool.getDescription()).isEqualTo("Old Desc");
    }

    @Test
    void testDeleteTool() {
        Tool tool = Tool.builder()
                .title("To Delete")
                .description("Desc")
                .pricePerDay(10.0)
                .district("Lisboa")
                .municipality("Lisboa")
                .owner(supplier)
                .active(true)
                .overallRating(0.0)
                .numRatings(0)
                .build();
        toolRepository.save(tool);

        restTemplate.delete("http://localhost:" + port + "/api/tools/" + tool.getId());

        assertThat(toolRepository.existsById(tool.getId())).isFalse();
    }
}
