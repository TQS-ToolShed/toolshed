package com.toolshed.backend.service;

import com.toolshed.backend.dto.AdminStatsDTO;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    public AdminStatsDTO getStats() {
        long totalUsers = userRepository.count();
        long totalBookings = bookingRepository.count();
        // Assuming you might want to filter by status for active/completed,
        // but for now we'll do simple counts or implementation specific logic if
        // repository supports it.
        // If repository doesn't have specific countByStatus methods, we might need to
        // add them or list all (inefficient).
        // For MVP, lets try to find if repository allows counting by status or just
        // return 0 for now if complex.
        // Actually, let's use what we have. If BookingRepository is JpaRepository, we
        // can validly assume countByStatus exists or add it.
        // I'll stick to simple counts for now to ensure it compiles, and I should check
        // BookingRepository content again or adding methods to it.

        return AdminStatsDTO.builder()
                .totalUsers(totalUsers)
                .activeUsers(userRepository.countByStatus(UserStatus.ACTIVE))
                .inactiveUsers(userRepository.countByStatus(UserStatus.SUSPENDED))
                .totalBookings(totalBookings)
                .activeBookings(bookingRepository.countByStatus(BookingStatus.APPROVED))
                .completedBookings(bookingRepository.countByStatus(BookingStatus.COMPLETED))
                .cancelledBookings(bookingRepository.countByStatus(BookingStatus.CANCELLED))
                .build();
    }

    public List<User> getAllUsers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() != com.toolshed.backend.repository.enums.UserRole.ADMIN)
                .toList();
    }

    public User activateUser(UUID userId) {
        User user = getUser(userId);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    public User deactivateUser(UUID userId) {
        User user = getUser(userId);
        user.setStatus(UserStatus.SUSPENDED);
        return userRepository.save(user);
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
