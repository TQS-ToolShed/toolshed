package com.toolshed.backend.config;

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
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class TestDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ToolRepository toolRepository;
    private final BookingRepository bookingRepository;

    @Override
    public void run(String... args) throws Exception {
        // Targeted Accounts
        String supplierEmail = "chalica@ua.pt";
        String renterEmail = "chalica2@ua.pt";

        if (bookingRepository.count() > 0) {
            System.out.println("Data likely already seeded (bookings exist). Skipping seeding.");
            return;
        }

        System.out.println("Seeding Test Data for Earnings Feature...");

        // 1. Ensure Users Exist
        User supplier = findOrCreateUser(supplierEmail, "Chalica", "Supplier", UserRole.SUPPLIER);
        User renter = findOrCreateUser(renterEmail, "Chalica", "Renter", UserRole.RENTER);

        // 2. Create Tools for Supplier
        List<Tool> tools = new ArrayList<>();
        tools.add(createTool(supplier, "Professional Power Drill", "High torque drill", 25.0));
        tools.add(createTool(supplier, "Heavy Duty Chainsaw", "Great for lumberjacks", 45.0));
        tools.add(createTool(supplier, "Tile Cutter", "Precise ceramic cutting", 15.0));
        tools.add(createTool(supplier, "Pressure Washer", "3000 PSI", 30.0));

        // 3. Create Past Bookings (Completed Payments)
        Random rand = new Random();
        int bookingsCreated = 0;

        // Generate bookings for the last 6 months
        for (int i = 0; i < 20; i++) {
            Tool tool = tools.get(rand.nextInt(tools.size()));

            // Random date in past 6 months
            int daysAgo = rand.nextInt(180) + 1;
            LocalDate endDate = LocalDate.now().minusDays(daysAgo);
            LocalDate startDate = endDate.minusDays(rand.nextInt(7) + 1); // 1-7 days duration

            long days = startDate.datesUntil(endDate.plusDays(1)).count();
            double totalPrice = days * tool.getPricePerDay();

            Booking booking = Booking.builder()
                    .tool(tool)
                    .renter(renter)
                    .owner(supplier)
                    .startDate(startDate)
                    .endDate(endDate)
                    .status(BookingStatus.COMPLETED) // Assuming enum exists, or PAST equivalent
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .totalPrice(totalPrice)
                    .build();

            bookingRepository.save(booking);
            bookingsCreated++;
        }

        System.out.println("Seeded " + bookingsCreated + " completed bookings for " + supplierEmail);
    }

    private User findOrCreateUser(String email, String firstName, String lastName, UserRole role) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            return existing.get();
        }

        User user = User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .password("chalica") // Password requested by user
                .role(role)
                .status(UserStatus.ACTIVE)
                .reputationScore(5.0)
                .walletBalance(0.0)
                .build();

        return userRepository.save(user);
    }

    private Tool createTool(User owner, String title, String desc, Double price) {
        Tool tool = Tool.builder()
                .title(title)
                .description(desc)
                .pricePerDay(price)
                .owner(owner)
                .district("Aveiro")
                .active(true)
                .overallRating(4.5)
                .numRatings(10)
                .build();
        return toolRepository.save(tool);
    }
}
