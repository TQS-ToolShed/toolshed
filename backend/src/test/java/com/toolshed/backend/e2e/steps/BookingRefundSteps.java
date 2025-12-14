package com.toolshed.backend.e2e.steps;

import com.toolshed.backend.dto.CancelBookingResponse;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.PayoutRepository;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.PaymentStatus;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;
import com.toolshed.backend.service.BookingService;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public class BookingRefundSteps {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PayoutRepository payoutRepository;

    private User renter;
    private User owner;
    private User otherRenter;
    private Tool tool;
    private Booking booking;
    private CancelBookingResponse cancelResponse;
    private ResponseStatusException exception;

    @Before("@refund")
    public void setUp() {
        payoutRepository.deleteAll();
        bookingRepository.deleteAll();
        toolRepository.deleteAll();
        userRepository.deleteAll();
        cancelResponse = null;
        exception = null;
    }

    @Given("a renter with email {string}")
    public void createRenter(String email) {
        renter = User.builder()
                .email(email)
                .firstName("Test")
                .lastName("Renter")
                .role(UserRole.RENTER)
                .status(UserStatus.ACTIVE)
                .password("password123")
                .walletBalance(0.0)
                .reputationScore(0.0)
                .build();
        renter = userRepository.save(renter);
    }

    @Given("an owner with email {string} and wallet balance {double}")
    public void createOwner(String email, Double walletBalance) {
        owner = User.builder()
                .email(email)
                .firstName("Test")
                .lastName("Owner")
                .role(UserRole.RENTER)
                .status(UserStatus.ACTIVE)
                .password("password123")
                .walletBalance(walletBalance)
                .reputationScore(0.0)
                .build();
        owner = userRepository.save(owner);
    }

    @Given("a tool {string} owned by the owner priced at {double} per day")
    public void createTool(String name, Double pricePerDay) {
        tool = Tool.builder()
                .title(name)
                .description("A powerful " + name.toLowerCase())
                .pricePerDay(pricePerDay)
                .owner(owner)
                .active(true)
                .overallRating(0.0)
                .numRatings(0)
                .district("Test District")
                .build();
        tool = toolRepository.save(tool);
    }

    @Given("a booking for the tool from the renter with total price {double} and status {string}")
    public void createBooking(Double totalPrice, String status) {
        booking = Booking.builder()
                .tool(tool)
                .renter(renter)
                .owner(owner)
                .totalPrice(totalPrice)
                .status(BookingStatus.valueOf(status))
                .paymentStatus(PaymentStatus.COMPLETED)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(12))
                .build();
        booking = bookingRepository.save(booking);
    }

    @Given("the booking starts in {int} days")
    public void setBookingStartDays(int days) {
        booking.setStartDate(LocalDate.now().plusDays(days));
        booking.setEndDate(LocalDate.now().plusDays(days + 2));
        booking = bookingRepository.save(booking);
    }

    @Given("the booking starts today")
    public void setBookingStartsToday() {
        booking.setStartDate(LocalDate.now());
        booking.setEndDate(LocalDate.now().plusDays(2));
        booking = bookingRepository.save(booking);
    }

    @Given("another renter with email {string}")
    public void createOtherRenter(String email) {
        otherRenter = User.builder()
                .email(email)
                .firstName("Other")
                .lastName("Renter")
                .role(UserRole.RENTER)
                .status(UserStatus.ACTIVE)
                .password("password123")
                .walletBalance(0.0)
                .reputationScore(0.0)
                .build();
        otherRenter = userRepository.save(otherRenter);
    }

    @Given("the booking is already cancelled")
    public void setBookingCancelled() {
        booking.setStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);
    }

    @When("the renter cancels the booking")
    public void renterCancelsBooking() {
        try {
            cancelResponse = bookingService.cancelBooking(booking.getId(), renter.getId());
        } catch (ResponseStatusException e) {
            exception = e;
        }
    }

    @When("the other renter tries to cancel the booking")
    public void otherRenterCancelsBooking() {
        try {
            cancelResponse = bookingService.cancelBooking(booking.getId(), otherRenter.getId());
        } catch (ResponseStatusException e) {
            exception = e;
        }
    }

    @Then("the booking status should be {string}")
    public void verifyBookingStatus(String expectedStatus) {
        if (cancelResponse != null) {
            assertThat(cancelResponse.getStatus()).isEqualTo(expectedStatus);
        }
        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(updatedBooking.getStatus().name()).isEqualTo(expectedStatus);
    }

    @Then("the refund percentage should be {int}")
    public void verifyRefundPercentage(int expectedPercentage) {
        assertThat(cancelResponse).isNotNull();
        assertThat(cancelResponse.getRefundPercentage()).isEqualTo(expectedPercentage);
    }

    @Then("the owner wallet balance should be {double}")
    public void verifyOwnerWalletBalance(Double expectedBalance) {
        User updatedOwner = userRepository.findById(owner.getId()).orElseThrow();
        assertThat(updatedOwner.getWalletBalance()).isEqualTo(expectedBalance);
    }

    @Then("a forbidden error should be returned")
    public void verifyForbiddenError() {
        assertThat(exception).isNotNull();
        assertThat(exception.getStatusCode().value()).isEqualTo(403);
    }

    @Then("a bad request error should be returned")
    public void verifyBadRequestError() {
        assertThat(exception).isNotNull();
        assertThat(exception.getStatusCode().value()).isEqualTo(400);
    }
}
