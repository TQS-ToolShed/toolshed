package com.toolshed.backend.e2e.steps;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import com.toolshed.backend.dto.BookingResponse;
import com.toolshed.backend.dto.ConditionReportRequest;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.ConditionStatus;
import com.toolshed.backend.repository.enums.DepositStatus;
import com.toolshed.backend.repository.enums.PaymentStatus;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;
import com.toolshed.backend.service.BookingService;
import com.toolshed.backend.service.PaymentService;
import com.toolshed.backend.service.PaymentServiceImpl.DepositNotRequiredException;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class ConditionReportSteps {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PaymentService paymentService;

    private User renter;
    private User owner;
    private Tool tool;
    private Booking booking;
    private BookingResponse bookingResponse;
    private Exception caughtException;
    private int lastHttpStatus;

    @Before("@condition-report or @deposit-payment")
    public void setUp() {
        bookingRepository.deleteAll();
        toolRepository.deleteAll();
        userRepository.deleteAll();
        caughtException = null;
        lastHttpStatus = 200;

        owner = new User();
        owner.setFirstName("Owner");
        owner.setLastName("User");
        owner.setEmail("owner@conditionreport.test");
        owner.setPassword("password");
        owner.setRole(UserRole.SUPPLIER);
        owner.setStatus(UserStatus.ACTIVE);
        owner.setReputationScore(5.0);
        owner = userRepository.save(owner);

        renter = new User();
        renter.setFirstName("Renter");
        renter.setLastName("User");
        renter.setEmail("renter@conditionreport.test");
        renter.setPassword("password");
        renter.setRole(UserRole.RENTER);
        renter.setStatus(UserStatus.ACTIVE);
        renter.setReputationScore(5.0);
        renter = userRepository.save(renter);

        tool = new Tool();
        tool.setTitle("Condition Report Test Tool");
        tool.setDescription("Tool for condition report testing");
        tool.setPricePerDay(20.0);
        tool.setDistrict("Test Location");
        tool.setOwner(owner);
        tool.setActive(true);
        tool.setOverallRating(0.0);
        tool.setNumRatings(0);
        tool = toolRepository.save(tool);
    }

    @Given("a completed booking exists with paid rental")
    public void a_completed_booking_exists_with_paid_rental() {
        booking = new Booking();
        booking.setTool(tool);
        booking.setRenter(renter);
        booking.setOwner(owner);
        booking.setStartDate(LocalDate.now().minusDays(5));
        booking.setEndDate(LocalDate.now().minusDays(1));
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setPaymentStatus(PaymentStatus.COMPLETED);
        booking.setTotalPrice(80.0);
        booking = bookingRepository.save(booking);
    }

    @Given("a completed booking exists with required deposit")
    public void a_completed_booking_exists_with_required_deposit() {
        booking = new Booking();
        booking.setTool(tool);
        booking.setRenter(renter);
        booking.setOwner(owner);
        booking.setStartDate(LocalDate.now().minusDays(5));
        booking.setEndDate(LocalDate.now().minusDays(1));
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setPaymentStatus(PaymentStatus.COMPLETED);
        booking.setTotalPrice(80.0);
        booking.setConditionStatus(ConditionStatus.BROKEN);
        booking.setDepositStatus(DepositStatus.REQUIRED);
        booking.setDepositAmount(50.0);
        booking = bookingRepository.save(booking);
    }

    @Given("a completed booking exists with no deposit required")
    public void a_completed_booking_exists_with_no_deposit_required() {
        booking = new Booking();
        booking.setTool(tool);
        booking.setRenter(renter);
        booking.setOwner(owner);
        booking.setStartDate(LocalDate.now().minusDays(5));
        booking.setEndDate(LocalDate.now().minusDays(1));
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setPaymentStatus(PaymentStatus.COMPLETED);
        booking.setTotalPrice(80.0);
        booking.setConditionStatus(ConditionStatus.OK);
        booking.setDepositStatus(DepositStatus.NOT_REQUIRED);
        booking.setDepositAmount(0.0);
        booking = bookingRepository.save(booking);
    }

    @Given("a booking exists that is not completed")
    public void a_booking_exists_that_is_not_completed() {
        booking = new Booking();
        booking.setTool(tool);
        booking.setRenter(renter);
        booking.setOwner(owner);
        booking.setStartDate(LocalDate.now().plusDays(1));
        booking.setEndDate(LocalDate.now().plusDays(3));
        booking.setStatus(BookingStatus.APPROVED);
        booking.setPaymentStatus(PaymentStatus.COMPLETED);
        booking.setTotalPrice(40.0);
        booking = bookingRepository.save(booking);
    }

    @When("the renter submits a condition report with status {string} and description {string}")
    public void the_renter_submits_a_condition_report_with_status_and_description(String status, String description) {
        try {
            ConditionReportRequest request = new ConditionReportRequest(
                    ConditionStatus.valueOf(status),
                    description,
                    renter.getId());
            bookingResponse = bookingService.submitConditionReport(booking.getId(), request);
            lastHttpStatus = 200;
        } catch (ResponseStatusException e) {
            caughtException = e;
            lastHttpStatus = e.getStatusCode().value();
        }
    }

    @When("the renter tries to submit a condition report")
    public void the_renter_tries_to_submit_a_condition_report() {
        try {
            ConditionReportRequest request = new ConditionReportRequest(
                    ConditionStatus.OK,
                    "Test report",
                    renter.getId());
            bookingResponse = bookingService.submitConditionReport(booking.getId(), request);
            lastHttpStatus = 200;
        } catch (ResponseStatusException e) {
            caughtException = e;
            lastHttpStatus = e.getStatusCode().value();
        }
    }

    @When("the renter tries to submit another condition report")
    public void the_renter_tries_to_submit_another_condition_report() {
        try {
            ConditionReportRequest request = new ConditionReportRequest(
                    ConditionStatus.BROKEN,
                    "Second report attempt",
                    renter.getId());
            bookingResponse = bookingService.submitConditionReport(booking.getId(), request);
            lastHttpStatus = 200;
        } catch (ResponseStatusException e) {
            caughtException = e;
            lastHttpStatus = e.getStatusCode().value();
        }
    }

    @When("the renter pays the deposit")
    public void the_renter_pays_the_deposit() {
        booking = bookingRepository.findById(booking.getId()).orElseThrow();
        booking = paymentService.markDepositAsPaid(booking.getId());
    }

    @When("the renter tries to pay the deposit")
    public void the_renter_tries_to_pay_the_deposit() {
        try {
            booking = paymentService.markDepositAsPaid(booking.getId());
        } catch (DepositNotRequiredException e) {
            caughtException = e;
        }
    }

    @When("the renter tries to pay the deposit again")
    public void the_renter_tries_to_pay_the_deposit_again() {
        try {
            paymentService.markDepositAsPaid(booking.getId());
        } catch (DepositNotRequiredException e) {
            caughtException = e;
        }
    }

    @Then("the condition report should be submitted successfully")
    public void the_condition_report_should_be_submitted_successfully() {
        assertThat(lastHttpStatus).isEqualTo(200);
        assertThat(bookingResponse).isNotNull();
    }

    @Then("the submission should fail with status {int}")
    public void the_submission_should_fail_with_status(Integer statusCode) {
        assertThat(lastHttpStatus).isEqualTo(statusCode);
    }

    @Then("the deposit status should be {string}")
    public void the_deposit_status_should_be(String expectedStatus) {
        assertThat(bookingResponse.getDepositStatus().name()).isEqualTo(expectedStatus);
    }

    @Then("the deposit amount should be {double}")
    public void the_deposit_amount_should_be(Double expectedAmount) {
        assertThat(bookingResponse.getDepositAmount()).isEqualTo(expectedAmount);
    }

    @Then("the deposit should be marked as paid")
    public void the_deposit_should_be_marked_as_paid() {
        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(updatedBooking.getDepositStatus()).isEqualTo(DepositStatus.PAID);
    }

    @Then("the deposit payment timestamp should be recorded")
    public void the_deposit_payment_timestamp_should_be_recorded() {
        Booking updatedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(updatedBooking.getDepositPaidAt()).isNotNull();
    }

    @Then("the payment should fail with deposit not required error")
    public void the_payment_should_fail_with_deposit_not_required_error() {
        assertThat(caughtException).isInstanceOf(DepositNotRequiredException.class);
        Booking currentBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(currentBooking.getDepositStatus()).isEqualTo(DepositStatus.NOT_REQUIRED);
    }

    @Then("the payment should fail with deposit already paid error")
    public void the_payment_should_fail_with_deposit_already_paid_error() {
        assertThat(caughtException).isInstanceOf(DepositNotRequiredException.class);
        Booking currentBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(currentBooking.getDepositStatus()).isEqualTo(DepositStatus.PAID);
    }

    @And("the final booking state shows condition {string} and deposit {string}")
    public void the_final_booking_state_shows_condition_and_deposit(String condition, String deposit) {
        Booking finalBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(finalBooking.getConditionStatus()).isEqualTo(ConditionStatus.valueOf(condition));
        assertThat(finalBooking.getDepositStatus()).isEqualTo(DepositStatus.valueOf(deposit));
    }
}
