package com.toolshed.backend.functional.steps;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolshed.backend.dto.CreateToolInput;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;

public class ToolImageSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private ResultActions resultActions;

    @Given("a supplier {string} exists with password {string}")
    public void supplier_exists(String email, String password) {
        if (userRepository.findByEmail(email).isEmpty()) {
            User user = User.builder()
                    .email(email)
                    .firstName("Test")
                    .lastName("Supplier")
                    .password(password)
                    .role(UserRole.SUPPLIER)
                    .status(UserStatus.ACTIVE)
                    .reputationScore(5.0)
                    .walletBalance(0.0)
                    .build();
            userRepository.save(user);
        }
    }

    @Given("I am authenticated as {string} with password {string}")
    public void i_am_authenticated(String email, String password) {
        // For MockMvc tests where we bypass actual login endpoint calling in this
        // specific flow,
        // we essentially ensure the user exists. If Spring Security is active, we might
        // need @WithMockUser
        // but explicit Login calls in BDD often imply obtaining a token.
        // For this step, we'll assume state preparation is enough as the controller
        // endpoint `createTool`
        // likely takes `supplierId` in BODY (as per CreateToolInput) rather than from
        // SecurityContext
        // OR the test environment mocks auth.
        // From CreateToolInput I see: private UUID supplierId;
        // So enforcing the user exists is key.
    }

    @When("I create a tool {string} with description {string} and price {double} and image {string}")
    public void i_create_a_tool_with_image(String title, String desc, Double price, String imageUrl) throws Exception {
        User supplier = userRepository.findByEmail("supplier@example.com").orElseThrow(); // Assumes set up in
                                                                                          // Background

        CreateToolInput input = CreateToolInput.builder()
                .title(title)
                .description(desc)
                .pricePerDay(price)
                .district("Aveiro")
                .supplierId(supplier.getId())
                .imageUrl(imageUrl)
                .build();

        resultActions = mockMvc.perform(post("/api/tools")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)));
    }

    @Then("the tool {string} should exist in my tools list")
    public void tool_should_exist(String title) throws Exception {
        resultActions.andExpect(status().isCreated());
        // Verify in DB
        Tool tool = toolRepository.findAll().stream()
                .filter(t -> t.getTitle().equals(title))
                .findFirst()
                .orElse(null);
        if (tool == null) {
            throw new AssertionError("Tool " + title + " was not found in DB");
        }
    }

    @Then("the tool {string} should have the image URL {string}")
    public void tool_should_have_image(String title, String imageUrl) {
        Tool tool = toolRepository.findAll().stream()
                .filter(t -> t.getTitle().equals(title))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Tool not found: " + title));

        if (!imageUrl.equals(tool.getImageUrl())) {
            throw new AssertionError("Expected image URL " + imageUrl + " but got " + tool.getImageUrl());
        }
    }

    @Given("a tool {string} exists with image {string} owned by {string}")
    public void tool_exists_with_image(String title, String imageUrl, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail).orElseThrow();

        Tool tool = Tool.builder()
                .title(title)
                .description("Description")
                .pricePerDay(10.0)
                .district("Aveiro")
                .owner(owner)
                .active(true)
                .imageUrl(imageUrl)
                .overallRating(0.0)
                .numRatings(0)
                .build();

        toolRepository.save(tool);
    }

    @When("I request the details for the tool {string}")
    public void request_tool_details(String title) throws Exception {
        Tool tool = toolRepository.findAll().stream()
                .filter(t -> t.getTitle().equals(title))
                .findFirst()
                .orElseThrow();

        resultActions = mockMvc.perform(get("/api/tools/{id}", tool.getId()));
    }

    @Then("the response should contain the image URL {string}")
    public void response_should_contain_image(String imageUrl) throws Exception {
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value(imageUrl));
    }
}
