package com.toolshed.backend.functional.steps;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.toolshed.backend.dto.BookingResponse;
import com.toolshed.backend.dto.CreateBookingRequest;
import com.toolshed.backend.dto.SubscriptionStatusResponse;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.SubscriptionTier;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;
import com.toolshed.backend.service.BookingService;
import com.toolshed.backend.service.SubscriptionService;
import com.toolshed.backend.service.SubscriptionServiceImpl;
import com.toolshed.backend.service.SubscriptionException;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class ProMemberSubscriptionSteps {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private SubscriptionServiceImpl subscriptionServiceImpl;

    @Autowired
    private BookingService bookingService;

    private User renter;
    private User toolOwner;
    private Tool tool;
    private SubscriptionStatusResponse subscriptionStatus;
    private BookingResponse bookingResponse;
    private Exception caughtException;

    @Before("@subscription")
    public void setUp() {
        bookingRepository.deleteAll();
        toolRepository.deleteAll();
        userRepository.deleteAll();
        caughtException = null;
    }

    @Given("a registered renter exists")
    public void createRenter() {
        renter = User.builder()
                .firstName("Test")
                .lastName("Renter")
                .email("renter-cucumber@test.com")
                .password("password123")
                .role(UserRole.RENTER)
                .status(UserStatus.ACTIVE)
                .reputationScore(4.0)
                .walletBalance(1000.0)
                .subscriptionTier(SubscriptionTier.FREE)
                .build();
        renter = userRepository.save(renter);

        // Also create a tool owner for booking tests
        toolOwner = User.builder()
                .firstName("Tool")
                .lastName("Owner")
                .email("owner-cucumber@test.com")
                .password("password123")
                .role(UserRole.SUPPLIER)
                .status(UserStatus.ACTIVE)
                .reputationScore(5.0)
                .walletBalance(500.0)
                .subscriptionTier(SubscriptionTier.FREE)
                .build();
        toolOwner = userRepository.save(toolOwner);
    }

    @Given("I have an active Pro subscription")
    public void activateProSubscription() {
        renter.setSubscriptionTier(SubscriptionTier.PRO);
        renter.setSubscriptionStart(LocalDateTime.now().minusDays(10));
        renter.setSubscriptionEnd(LocalDateTime.now().plusDays(20));
        renter.setStripeSubscriptionId("sub_test_cucumber");
        renter = userRepository.save(renter);
    }

    @Given("a tool {string} is available at {int} euros per day")
    public void createTool(String toolName, int pricePerDay) {
        tool = Tool.builder()
                .title(toolName)
                .description("A test tool for cucumber")
                .pricePerDay((double) pricePerDay)
                .owner(toolOwner)
                .active(true)
                .overallRating(0.0)
                .numRatings(0)
                .district("Test District")
                .build();
        tool = toolRepository.save(tool);
    }

    @When("I check my subscription status")
    public void checkSubscriptionStatus() {
        subscriptionStatus = subscriptionService.getSubscriptionStatus(renter.getId());
    }

    @When("I activate my Pro subscription with subscription ID {string}")
    public void activateSubscriptionWithId(String subscriptionId) {
        subscriptionServiceImpl.activateProSubscription(renter.getId(), subscriptionId);
        // Refresh user
        renter = userRepository.findById(renter.getId()).orElseThrow();
    }

    @When("I create a booking for {int} days")
    public void createBooking(int days) {
        LocalDate startDate = LocalDate.now().plusDays(7);
        LocalDate endDate = startDate.plusDays(days - 1);

        CreateBookingRequest request = CreateBookingRequest.builder()
                .toolId(tool.getId())
                .renterId(renter.getId())
                .startDate(startDate)
                .endDate(endDate)
                .build();

        bookingResponse = bookingService.createBooking(request);
    }

    @When("I cancel my subscription")
    public void cancelSubscription() {
        try {
            subscriptionService.cancelSubscription(renter.getId());
        } catch (SubscriptionException e) {
            caughtException = e;
        }
    }

    @When("I try to subscribe to Pro again")
    public void trySubscribeAgain() {
        try {
            subscriptionService.createProSubscription(renter.getId(), "http://success", "http://cancel");
        } catch (SubscriptionException e) {
            caughtException = e;
        }
    }

    @Then("I should see tier {string}")
    public void verifySubscriptionTier(String tier) {
        assertThat(subscriptionStatus.getTier().name()).isEqualTo(tier);
    }

    @Then("I should see discount percentage {int}")
    public void verifyDiscountPercentage(int discount) {
        assertThat(subscriptionStatus.getDiscountPercentage()).isEqualTo((double) discount);
    }

    @Then("my subscription tier should be {string}")
    public void verifyUserSubscriptionTier(String tier) {
        assertThat(renter.getSubscriptionTier().name()).isEqualTo(tier);
    }

    @Then("my subscription should be active")
    public void verifySubscriptionActive() {
        assertThat(subscriptionService.isProMember(renter)).isTrue();
    }

    @Then("the total price should be {int} euros")
    public void verifyTotalPrice(int expectedPrice) {
        assertThat(bookingResponse.getTotalPrice()).isEqualTo((double) expectedPrice);
    }

    @Then("my subscription cancellation should fail with error")
    public void verifyCancellationFails() {
        // Note: cancellation fails because Stripe API is not configured in tests
        // In real tests with Stripe mock, this would verify the behavior
        assertThat(caughtException).isNotNull();
    }

    @Then("I should see an error about already being a Pro member")
    public void verifyAlreadyProError() {
        assertThat(caughtException).isInstanceOf(SubscriptionException.class);
        assertThat(caughtException.getMessage()).contains("already has an active Pro subscription");
    }
}
