package com.toolshed.backend.functional.steps;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolshed.backend.dto.CreateReviewRequest;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ReviewRepository;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.ReviewType;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class ReviewSteps {

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

    private User renter;
    private User owner;
    private Tool tool;
    private Booking booking;
    private ResultActions resultActions;

    @Before("@review")
    public void setUp() {
        reviewRepository.deleteAll();
        bookingRepository.deleteAll();
        toolRepository.deleteAll();
        userRepository.deleteAll();

        owner = new User();
        owner.setFirstName("Owner");
        owner.setLastName("User");
        owner.setEmail("owner@example.com");
        owner.setPassword("password");
        owner.setRole(UserRole.SUPPLIER);
        owner.setStatus(UserStatus.ACTIVE);
        owner.setReputationScore(5.0);
        owner = userRepository.save(owner);

        renter = new User();
        renter.setFirstName("Renter");
        renter.setLastName("User");
        renter.setEmail("renter@example.com");
        renter.setPassword("password");
        renter.setRole(UserRole.RENTER);
        renter.setStatus(UserStatus.ACTIVE);
        renter.setReputationScore(5.0);
        renter = userRepository.save(renter);

        tool = new Tool();
        tool.setTitle("Test Tool");
        tool.setDescription("Description");
        tool.setPricePerDay(10.0);
        tool.setLocation("Location");
        tool.setOwner(owner);
        tool.setActive(true);
        tool.setOverallRating(0.0);
        tool.setNumRatings(0);
        tool = toolRepository.save(tool);
    }

    @Given("a completed booking exists for a tool")
    public void a_completed_booking_exists_for_a_tool() {
        booking = new Booking();
        booking.setTool(tool);
        booking.setRenter(renter);
        booking.setOwner(owner);
        booking.setStartDate(LocalDate.now().minusDays(5));
        booking.setEndDate(LocalDate.now().minusDays(3));
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setTotalPrice(30.0);
        booking = bookingRepository.save(booking);
    }

    @When("the renter submits a review for the owner with rating {int} and comment {string}")
    public void the_renter_submits_a_review_for_the_owner_with_rating_and_comment(Integer rating, String comment)
            throws Exception {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setBookingId(booking.getId());
        request.setRating(rating);
        request.setComment(comment);
        request.setType(ReviewType.RENTER_TO_OWNER);

        resultActions = mockMvc.perform(post("/api/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    @When("the owner submits a review for the renter with rating {int} and comment {string}")
    public void the_owner_submits_a_review_for_the_renter_with_rating_and_comment(Integer rating, String comment)
            throws Exception {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setBookingId(booking.getId());
        request.setRating(rating);
        request.setComment(comment);
        request.setType(ReviewType.OWNER_TO_RENTER);

        resultActions = mockMvc.perform(post("/api/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    @When("the renter submits a review for the tool with rating {int} and comment {string}")
    public void the_renter_submits_a_review_for_the_tool_with_rating_and_comment(Integer rating, String comment)
            throws Exception {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setBookingId(booking.getId());
        request.setRating(rating);
        request.setComment(comment);
        request.setType(ReviewType.RENTER_TO_TOOL);

        resultActions = mockMvc.perform(post("/api/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    @Then("the review should be created successfully")
    public void the_review_should_be_created_successfully() throws Exception {
        resultActions.andExpect(status().isOk());
    }

    @Then("the review details should contain rating {int} and comment {string}")
    public void the_review_details_should_contain_rating_and_comment(Integer rating, String comment) throws Exception {
        resultActions
                .andExpect(jsonPath("$.rating").value(rating))
                .andExpect(jsonPath("$.comment").value(comment));
    }
}
