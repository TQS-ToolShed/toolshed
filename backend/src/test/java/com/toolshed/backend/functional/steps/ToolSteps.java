package com.toolshed.backend.functional.steps;

import static org.hamcrest.Matchers.hasItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolshed.backend.dto.CreateToolInput;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ReviewRepository;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class ToolSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private CreateToolInput toolRequest;
    private ResultActions resultActions;

    @Before("@tool")
    public void setUp() {
        reviewRepository.deleteAll();
        bookingRepository.deleteAll();
        toolRepository.deleteAll();
        userRepository.deleteAll();
        
        testUser = new User();
        testUser.setFirstName("Test");
        testUser.setLastName("Supplier");
        testUser.setEmail("supplier@example.com");
        testUser.setPassword("password");
        testUser.setRole(UserRole.SUPPLIER);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setReputationScore(5.0);
        testUser = userRepository.save(testUser);
        
        toolRequest = new CreateToolInput();
    }

    @Given("I am on the create tool page")
    public void i_am_on_the_create_tool_page() {
        // No-op for API test
    }

    @When("I fill in the tool details with title {string}, description {string}, price {string}, district {string}")
    public void i_fill_in_the_tool_details(String title, String description, String price, String district) {
        toolRequest.setTitle(title);
        toolRequest.setDescription(description);
        toolRequest.setPricePerDay(Double.parseDouble(price));
        toolRequest.setDistrict(district.trim());
        toolRequest.setSupplierId(testUser.getId());
    }

    @When("I submit the form")
    public void i_submit_the_form() throws Exception {
        resultActions = mockMvc.perform(post("/api/tools")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(toolRequest)));
    }

    @Then("I should see {string} in the tool list")
    public void i_should_see_in_the_tool_list(String title) throws Exception {
        mockMvc.perform(get("/api/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem(title)));
    }

    @Given("there is a tool with title {string}")
    public void there_is_a_tool_with_title(String title) {
        Tool tool = new Tool();
        tool.setTitle(title);
        tool.setDescription("Seeded tool");
        tool.setPricePerDay(10.0);
        tool.setDistrict("Porto");
        tool.setOwner(testUser);
        tool.setActive(true);
        tool.setOverallRating(0.0);
        tool.setNumRatings(0);
        toolRepository.save(tool);
    }

    @When("I search for {string}")
    public void i_search_for(String keyword) throws Exception {
        resultActions = mockMvc.perform(get("/api/tools/search")
                .param("keyword", keyword));
    }
    
    @Then("I should see the tool details")
    public void i_should_see_the_tool_details() throws Exception {
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").isNotEmpty());
    }

    @Then("I should see {string} in the results")
    public void i_should_see_in_the_results(String title) throws Exception {
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem(title)));
    }
}
