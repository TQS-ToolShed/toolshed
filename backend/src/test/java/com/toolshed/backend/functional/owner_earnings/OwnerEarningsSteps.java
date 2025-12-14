package com.toolshed.backend.functional.owner_earnings;

import com.toolshed.backend.dto.MonthlyEarningsResponse;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.PaymentStatus;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;
import com.toolshed.backend.service.PaymentService;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class OwnerEarningsSteps {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private User owner;
    private List<MonthlyEarningsResponse> earningsResponse;

    @Before("@owner-earnings")
    public void setUp() {
        bookingRepository.deleteAll();
        toolRepository.deleteAll();
        userRepository.deleteAll();
        earningsResponse = null;
    }

    @Given("a user exists for earnings monitoring with email {string} and role {string}")
    public void a_user_exists_for_earnings_monitoring_with_email_and_role(String email, String role) {
        if (userRepository.findByEmail(email).isPresent()) {
            userRepository.delete(userRepository.findByEmail(email).get());
        }

        owner = User.builder()
                .firstName("Test")
                .lastName("User")
                .email(email)
                .password("password")
                .role(UserRole.valueOf(role))
                .status(UserStatus.ACTIVE)
                .reputationScore(5.0)
                .walletBalance(0.0)
                .build();
        owner = userRepository.save(owner);
    }

    @Given("the owner has the following completed bookings:")
    public void the_owner_has_the_following_completed_bookings(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);

        for (Map<String, String> row : rows) {
            String toolName = row.get("tool_name");
            String month = row.get("month");
            int year = Integer.parseInt(row.get("year"));
            double amount = Double.parseDouble(row.get("amount"));

            // Create Tool if not exists or multiple tools logic
            // For simplicity, create a new tool for each row or reuse if name matches?
            // Let's create unique tools per name for this simple test.
            Tool tool = toolRepository.save(Tool.builder()
                    .title(toolName)
                    .description("Desc")
                    .pricePerDay(10.0)
                    .owner(owner)
                    .active(true)
                    .overallRating(5.0)
                    .numRatings(1)
                    .district("Aveiro")
                    .build());

            // Determine date from month/year
            // Month is string "MARCH", "FEBRUARY"
            int monthValue = java.time.Month.valueOf(month).getValue();
            LocalDate endDate = LocalDate.of(year, monthValue, 15);
            LocalDate startDate = endDate.minusDays(1);

            // Create Booking
            Booking booking = Booking.builder()
                    .tool(tool)
                    .owner(owner)
                    .renter(owner) // Self-renting for simplicity or we need another user?
                                   // Service logic doesn't care who the renter is, only owner earning.
                                   // But Booking needs a renter. Let's use the owner as renter to simplify
                                   // or create a dummy renter.
                                   // Existing steps created a separate renter. Let's create a dummy renter once.
                    .status(BookingStatus.COMPLETED)
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .totalPrice(amount)
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();

            // To be valid, we need a renter.
            // Let's find or create a renter.
            User renter = userRepository.findByEmail("renter@dummy.com")
                    .orElseGet(() -> userRepository.save(User.builder()
                            .email("renter@dummy.com")
                            .firstName("Renter")
                            .lastName("Dummy")
                            .role(UserRole.RENTER)
                            .password("pass")
                            .reputationScore(5.0)
                            .walletBalance(0.0)
                            .build()));
            booking.setRenter(renter);

            bookingRepository.save(booking);
        }
    }

    @When("the owner requests their monthly earnings")
    public void the_owner_requests_their_monthly_earnings() {
        earningsResponse = paymentService.getMonthlyEarnings(owner.getId());
    }

    @Then("the earnings response should contain {int} entries")
    public void the_earnings_response_should_contain_entries(int count) {
        assertThat(earningsResponse).isNotNull();
        assertThat(earningsResponse).hasSize(count);
    }

    @And("the earnings for {string} {string} should be {double}")
    public void the_earnings_for_should_be(String month, String year, double amount) {
        int yearInt = Integer.parseInt(year);

        MonthlyEarningsResponse match = earningsResponse.stream()
                .filter(e -> e.getMonth().equals(month) && e.getYear() == yearInt)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Entry not found for " + month + " " + year));

        assertThat(match.getAmount()).isEqualTo(amount);
    }
}
