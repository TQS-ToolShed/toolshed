package com.toolshed.backend.config;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ReviewRepository;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.entities.Review;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.ConditionStatus;
import com.toolshed.backend.repository.enums.DepositStatus;
import com.toolshed.backend.repository.enums.PaymentStatus;
import com.toolshed.backend.repository.enums.ReviewType;
import com.toolshed.backend.repository.enums.UserRole;
import com.toolshed.backend.repository.enums.UserStatus;

import lombok.RequiredArgsConstructor;

@Component
@org.springframework.context.annotation.Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEFAULT_PASSWORD = "password";

    private final UserRepository userRepository;
    private final ToolRepository toolRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            logger.info("Database already populated. Skipping data seeding.");
            return;
        }

        logger.info("Starting data seeding...");

        createAdmin();
        List<User> renters = createRenters();
        List<User> owners = createOwners();
        List<Tool> allTools = createTools(owners);
        createBookingsAndReviews(renters, allTools);

        logger.info("Data seeding completed.");
    }

    private void createAdmin() {
        User admin = User.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin@toolshed.com")
                .password(DEFAULT_PASSWORD)
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .reputationScore(5.0)
                .walletBalance(0.0)
                .build();
        userRepository.save(admin);
    }

    private List<User> createRenters() {
        List<User> renters = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            User renter = User.builder()
                    .firstName("Renter")
                    .lastName("One" + i)
                    .email("renter" + i + "@toolshed.com")
                    .password(DEFAULT_PASSWORD)
                    .role(UserRole.RENTER)
                    .status(UserStatus.ACTIVE)
                    .reputationScore(5.0)
                    .walletBalance(1000.0)
                    .build();
            renters.add(userRepository.save(renter));
        }
        return renters;
    }

    private List<User> createOwners() {
        List<User> owners = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            User owner = User.builder()
                    .firstName("Owner")
                    .lastName("Two" + i)
                    .email("owner" + i + "@toolshed.com")
                    .password(DEFAULT_PASSWORD)
                    .role(UserRole.SUPPLIER)
                    .status(UserStatus.ACTIVE)
                    .reputationScore(5.0)
                    .walletBalance(0.0)
                    .build();
            owners.add(userRepository.save(owner));
        }
        return owners;
    }

    private List<Tool> createTools(List<User> owners) {
        List<Tool> allTools = new ArrayList<>();
        String[] toolNames = { "Drill", "Saw", "Hammer", "Ladder", "Wrench", "Sander", "Mower", "Blower", "Trimmer" };
        int nameIndex = 0;

        for (User owner : owners) {
            for (int k = 0; k < 3; k++) {
                Tool tool = Tool.builder()
                        .title(toolNames[nameIndex++ % toolNames.length] + " " + (k + 1))
                        .description("High quality tool for your needs.")
                        .pricePerDay(10.0 + k * 5)
                        .district(k % 2 == 0 ? "Aveiro" : "Porto")
                        .owner(owner)
                        .active(true)
                        .overallRating(0.0)
                        .numRatings(0)
                        .imageUrl("https://placehold.co/400")
                        .build();
                allTools.add(toolRepository.save(tool));
            }
        }
        return allTools;
    }

    private void createBookingsAndReviews(List<User> renters, List<Tool> tools) {
        SecureRandom random = new SecureRandom();
        int problemCount = 0;

        for (int i = 0; i < 15; i++) {
            User renter = renters.get(random.nextInt(renters.size()));
            Tool tool = tools.get(random.nextInt(tools.size()));
            User owner = tool.getOwner();

            LocalDate endDate = LocalDate.now().minusDays((long) random.nextInt(30) + 1);
            LocalDate startDate = endDate.minusDays((long) random.nextInt(5) + 1);
            double totalPrice = tool.getPricePerDay() * (endDate.toEpochDay() - startDate.toEpochDay() + 1);

            Booking booking = Booking.builder()
                    .tool(tool)
                    .owner(owner)
                    .renter(renter)
                    .startDate(startDate)
                    .endDate(endDate)
                    .totalPrice(totalPrice)
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .build();

            problemCount = handleBookingStatus(booking, i, problemCount, owner, totalPrice);

            booking = bookingRepository.save(booking);

            handleReviews(booking, renter, owner, tool, random);
        }
    }

    private int handleBookingStatus(Booking booking, int index, int problemCount, User owner, double totalPrice) {
        if (problemCount < 4 && index > 10) {
            problemCount++;
            if (problemCount % 2 == 0) {
                // Damaged
                booking.setStatus(BookingStatus.COMPLETED);
                booking.setConditionStatus(ConditionStatus.BROKEN);
                booking.setConditionDescription("Tool returned damaged.");
                booking.setDepositStatus(DepositStatus.REQUIRED);
                booking.setConditionReportedBy(owner);
                booking.setConditionReportedAt(LocalDateTime.now().minusDays(1));
            } else {
                // Cancelled or issues
                booking.setStatus(BookingStatus.CANCELLED);
                booking.setCancelledAt(LocalDateTime.now().minusDays(10));
                booking.setRefundAmount(totalPrice);
                booking.setPaymentStatus(PaymentStatus.REFUNDED);
            }
        } else {
            // Happy path
            booking.setStatus(BookingStatus.COMPLETED);
            booking.setConditionStatus(ConditionStatus.OK);
            booking.setDepositStatus(DepositStatus.NOT_REQUIRED);
        }
        return problemCount;
    }

    private void handleReviews(Booking booking, User renter, User owner, Tool tool, SecureRandom random) {
        if (booking.getStatus() == BookingStatus.COMPLETED && random.nextBoolean()) {
            // Renter reviews Tool
            Review toolReview = Review.builder()
                    .booking(booking)
                    .reviewer(renter)
                    .tool(tool)
                    .type(ReviewType.RENTER_TO_TOOL)
                    .rating(random.nextInt(2) + 4) // 4 or 5
                    .comment("Great tool!")
                    .build();
            reviewRepository.save(toolReview);

            updateToolRating(tool, toolReview.getRating());

            // Owner reviews Renter
            Review renterReview = Review.builder()
                    .booking(booking)
                    .reviewer(owner)
                    .type(ReviewType.OWNER_TO_RENTER)
                    .rating(5)
                    .comment("Great renter, returned on time.")
                    .build();
            reviewRepository.save(renterReview);

            // Renter reviews Owner
            Review ownerReview = Review.builder()
                    .booking(booking)
                    .reviewer(renter)
                    .owner(owner)
                    .type(ReviewType.RENTER_TO_OWNER)
                    .rating(5)
                    .comment("Nice owner.")
                    .build();
            reviewRepository.save(ownerReview);
        }
    }

    private void updateToolRating(Tool tool, int newRating) {
        double currentTotal = tool.getOverallRating() * tool.getNumRatings();
        tool.setNumRatings(tool.getNumRatings() + 1);
        tool.setOverallRating((currentTotal + newRating) / tool.getNumRatings());
        toolRepository.save(tool);
    }
}
