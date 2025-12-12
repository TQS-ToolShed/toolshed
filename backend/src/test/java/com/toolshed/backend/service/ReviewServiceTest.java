package com.toolshed.backend.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.toolshed.backend.dto.CreateReviewRequest;
import com.toolshed.backend.dto.ReviewResponse;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ReviewRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.entities.Review;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.ReviewType;

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
        CreateReviewRequest request = new CreateReviewRequest(booking.getId(), ReviewType.RENTER_TO_OWNER, 5, "Great!");

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

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
        CreateReviewRequest request = new CreateReviewRequest(booking.getId(), ReviewType.RENTER_TO_OWNER, 5, "Great!");

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> reviewService.createReview(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Booking must be completed to leave a review");
    }

    @Test
    void createReview_reviewAlreadyExists_throwsException() {
        CreateReviewRequest request = new CreateReviewRequest(booking.getId(), ReviewType.RENTER_TO_OWNER, 5, "Great!");

        Review existingReview = Review.builder().type(ReviewType.RENTER_TO_OWNER).build();
        booking.setReviews(List.of(existingReview));

        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> reviewService.createReview(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Review of this type already exists for this booking");
    }

    @Test
    void updateReview_validRequest_returnsUpdatedResponse() {
        UUID reviewId = UUID.randomUUID();

        Review existingReview = Review.builder()
                .id(reviewId)
                .booking(booking)
                .reviewer(renter)
                .owner(owner)
                .tool(tool)
                .rating(3)
                .comment("Original comment")
                .type(ReviewType.RENTER_TO_OWNER)
                .build();

        CreateReviewRequest request = new CreateReviewRequest(booking.getId(), ReviewType.RENTER_TO_OWNER, 5,
                "Updated comment");

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview));

        Review updatedReview = Review.builder()
                .id(reviewId)
                .booking(booking)
                .reviewer(renter)
                .owner(owner)
                .tool(tool)
                .rating(5)
                .comment("Updated comment")
                .type(ReviewType.RENTER_TO_OWNER)
                .build();

        when(reviewRepository.save(any(Review.class))).thenReturn(updatedReview);

        ReviewResponse response = reviewService.updateReview(reviewId, request);

        assertThat(response).isNotNull();
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getComment()).isEqualTo("Updated comment");
        verify(reviewRepository).findById(reviewId);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void updateReview_reviewNotFound_throwsException() {
        UUID reviewId = UUID.randomUUID();
        CreateReviewRequest request = new CreateReviewRequest(booking.getId(), ReviewType.RENTER_TO_OWNER, 5,
                "Updated comment");

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.updateReview(reviewId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Review not found");
    }
}
