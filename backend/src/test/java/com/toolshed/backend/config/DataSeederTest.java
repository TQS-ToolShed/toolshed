package com.toolshed.backend.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ReviewRepository;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.entities.Review;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ToolRepository toolRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Test
    @DisplayName("Should populate database when it is empty")
    void run_emptyDatabase_populatesData() throws Exception {
        // Arrange
        when(userRepository.count()).thenReturn(0L);

        // Mock save calls to return the entity (stubbing basic behavior)
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);
        when(toolRepository.save(any(Tool.class))).thenAnswer(i -> i.getArguments()[0]);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArguments()[0]);
        // Review saving might happen depending on random chance, so we can't strictly
        // enforce it unless we mock Random
        // But we can verify it doesn't crash effectively.
        // If we want to be strict about reviewrepo saving, we'd need to mock it too if
        // it's called.
        // Since we are using lenient mocks by default in many setups, or just standard
        // mocks,
        // if it IS called and not stubbed, it returns null, which might cause issues if
        // return value is used.
        // The code ignores return value of reviewRepository.save.

        // Act
        dataSeeder.run();

        // Assert
        // 1 Admin + 3 Renters + 3 Owners = 7 Users
        verify(userRepository, atLeastOnce()).save(any(User.class));

        // 3 Owners * 3 Tools = 9 Tools
        verify(toolRepository, atLeastOnce()).save(any(Tool.class));

        // 15 Bookings
        verify(bookingRepository, atLeastOnce()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should not populate database when it already has users")
    void run_populatedDatabase_doesNothing() throws Exception {
        // Arrange
        when(userRepository.count()).thenReturn(1L);

        // Act
        dataSeeder.run();

        // Assert
        verify(userRepository, never()).save(any(User.class));
        verify(toolRepository, never()).save(any(Tool.class));
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(reviewRepository, never()).save(any(Review.class));
    }
}
