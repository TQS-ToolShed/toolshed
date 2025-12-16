package com.toolshed.backend.functional.steps;

import com.toolshed.backend.dto.PayoutResponse;
import com.toolshed.backend.dto.WalletResponse;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.PayoutRepository;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.entities.Payout;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.PaymentStatus;
import com.toolshed.backend.repository.enums.PayoutStatus;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;
import com.toolshed.backend.service.PaymentService;
import com.toolshed.backend.service.PaymentServiceImpl.InsufficientBalanceException;
import com.toolshed.backend.service.PaymentServiceImpl.InvalidPayoutException;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class OwnerPayoutSteps {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PayoutRepository payoutRepository;

    private User owner;
    private User renter;
    private Tool tool;
    private Booking booking;
    private WalletResponse walletResponse;
    private PayoutResponse payoutResponse;
    private List<PayoutResponse> payoutHistory;
    private Exception thrownException;

    @Before("@owner-payout")
    public void setUp() {
        payoutRepository.deleteAll();
        bookingRepository.deleteAll();
        toolRepository.deleteAll();
        userRepository.deleteAll();

        thrownException = null;
        walletResponse = null;
        payoutResponse = null;
        payoutHistory = null;
    }

    @Given("a user exists with email {string} and role {string}")
    public void aUserExistsWithEmailAndRole(String email, String role) {
        owner = userRepository.save(User.builder()
                .email(email)
                .firstName("Test")
                .lastName("Owner")
                .password("password123")
                .role(UserRole.valueOf(role))
                .status(UserStatus.ACTIVE)
                .walletBalance(0.0)
                .reputationScore(5.0)
                .build());
    }

    @And("the owner has wallet balance of {double} euros")
    public void theOwnerHasWalletBalanceOfEuros(double balance) {
        owner.setWalletBalance(balance);
        owner = userRepository.save(owner);
    }

    @When("the owner requests their wallet information")
    public void theOwnerRequestsTheirWalletInformation() {
        walletResponse = paymentService.getOwnerWallet(owner.getId());
    }

    @Then("the wallet response should contain balance {double}")
    public void theWalletResponseShouldContainBalance(double expectedBalance) {
        assertThat(walletResponse).isNotNull();
        assertThat(walletResponse.getBalance()).isEqualTo(expectedBalance);
    }

    @And("the wallet response should contain recent payouts list")
    public void theWalletResponseShouldContainRecentPayoutsList() {
        assertThat(walletResponse.getRecentPayouts()).isNotNull();
    }

    @When("the owner requests a payout of {double} euros")
    public void theOwnerRequestsAPayoutOfEuros(double amount) {
        try {
            payoutResponse = paymentService.requestPayout(owner.getId(), amount);
        } catch (InsufficientBalanceException | InvalidPayoutException e) {
            thrownException = e;
        }
    }

    @Then("the payout request should be successful")
    public void thePayoutRequestShouldBeSuccessful() {
        assertThat(thrownException).isNull();
        assertThat(payoutResponse).isNotNull();
    }

    @And("the payout status should be {string}")
    public void thePayoutStatusShouldBe(String status) {
        assertThat(payoutResponse.getStatus()).isEqualTo(PayoutStatus.valueOf(status));
    }

    @And("the owner's wallet balance should be {double} euros")
    public void theOwnersWalletBalanceShouldBeEuros(double expectedBalance) {
        User updatedOwner = userRepository.findById(owner.getId()).orElseThrow();
        assertThat(updatedOwner.getWalletBalance()).isEqualTo(expectedBalance);
    }

    @Then("the payout request should fail with {string}")
    public void thePayoutRequestShouldFailWith(String errorMessage) {
        assertThat(thrownException).isNotNull();
        assertThat(thrownException.getMessage()).contains(errorMessage);
    }

    @Given("a booking exists for a tool owned by the owner with total price {double}")
    public void aBookingExistsForAToolOwnedByTheOwnerWithTotalPrice(double totalPrice) {
        // Create a renter
        renter = userRepository.save(User.builder()
                .email("renter@test.com")
                .firstName("Test")
                .lastName("Renter")
                .password("password123")
                .role(UserRole.RENTER)
                .status(UserStatus.ACTIVE)
                .reputationScore(5.0)
                .build());

        // Create a tool owned by the owner
        tool = toolRepository.save(Tool.builder()
                .title("Test Tool")
                .description("A test tool")
                .pricePerDay(25.0)
                .district("Test Location")
                .owner(owner)
                .active(true)
                .overallRating(0.0)
                .numRatings(0)
                .build());

        // Create a booking
        booking = bookingRepository.save(Booking.builder()
                .tool(tool)
                .renter(renter)
                .owner(owner)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .totalPrice(totalPrice)
                .status(BookingStatus.APPROVED)
                .paymentStatus(PaymentStatus.PENDING)
                .build());
    }

    @When("the booking is marked as paid")
    public void theBookingIsMarkedAsPaid() {
        paymentService.markBookingAsPaid(booking.getId());
    }

    @Given("the owner has made previous payouts")
    public void theOwnerHasMadePreviousPayouts() {
        // Create some previous payouts
        payoutRepository.save(Payout.builder()
                .owner(owner)
                .amount(50.0)
                .status(PayoutStatus.COMPLETED)
                .stripeTransferId("tr_test_1")
                .requestedAt(LocalDateTime.now().minusDays(2))
                .completedAt(LocalDateTime.now().minusDays(2))
                .build());

        payoutRepository.save(Payout.builder()
                .owner(owner)
                .amount(30.0)
                .status(PayoutStatus.COMPLETED)
                .stripeTransferId("tr_test_2")
                .requestedAt(LocalDateTime.now().minusDays(1))
                .completedAt(LocalDateTime.now().minusDays(1))
                .build());
    }

    @When("the owner requests their payout history")
    public void theOwnerRequestsTheirPayoutHistory() {
        payoutHistory = paymentService.getPayoutHistory(owner.getId());
    }

    @Then("the payout history should contain the previous payouts")
    public void thePayoutHistoryShouldContainThePreviousPayouts() {
        assertThat(payoutHistory).isNotNull();
        assertThat(payoutHistory).hasSizeGreaterThanOrEqualTo(2);
    }
}
