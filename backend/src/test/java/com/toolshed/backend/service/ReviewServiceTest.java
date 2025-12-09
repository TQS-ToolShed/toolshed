package com.toolshed.backend.service;

import com.toolshed.backend.dto.CreateReviewRequest;
import com.toolshed.backend.dto.ReviewResponse;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ReviewRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.entities.Review;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.BookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private Booking booking;
    private User renter;
    private User owner;
    private Tool tool;

    @BeforeEach
    void setUp() {
        renter = User.builder().id(UUID.randomUUID()).firstName("Renter").lastName("User").build();
        owner = User.builder().id(UUID.randomUUID()).firstName("Owner").lastName("User").build();
        tool = Tool.builder().id(UUID.randomUUID()).owner(owner).build();
        
        booking = Booking.builder()
                .id(UUID.randomUUID())
                .renter(renter)
                .owner(owner)
                .tool(tool)
                .status(BookingStatus.COMPLETED)
                .build();
    }

    @Test
    void createReview_validRequest_returnsResponse() {
        CreateReviewRequest request = new CreateReviewRequest(booking.getId(), 5, "Great!");

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingId(booking.getId())).thenReturn(false);
        
        Review savedReview = Review.builder()
                .id(UUID.randomUUID())
                .booking(booking)
                .reviewer(renter)
                .owner(owner)
                .tool(tool)
                .rating(5)
                .comment("Great!")
                .build();
                
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        ReviewResponse response = reviewService.createReview(request);

        assertThat(response).isNotNull();
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getComment()).isEqualTo("Great!");
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReview_bookingNotCompleted_throwsException() {
        booking.setStatus(BookingStatus.APPROVED);
        CreateReviewRequest request = new CreateReviewRequest(booking.getId(), 5, "Great!");

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> reviewService.createReview(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Booking must be completed to leave a review");
    }

    @Test
    void createReview_reviewAlreadyExists_throwsException() {
        CreateReviewRequest request = new CreateReviewRequest(booking.getId(), 5, "Great!");

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingId(booking.getId())).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Review already exists for this booking");
    }
}
