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

        @Mock
        private com.toolshed.backend.repository.UserRepository userRepository;

        @Mock
        private com.toolshed.backend.repository.ToolRepository toolRepository;

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
                CreateReviewRequest request = new CreateReviewRequest(booking.getId(), ReviewType.RENTER_TO_OWNER, 5,
                                "Great!");

                when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

                Review savedReview = Review.builder()
                                .id(UUID.randomUUID())
                                .booking(booking)
                                .reviewer(renter)
                                .owner(owner) // Owner is target
                                .tool(tool)
                                .rating(5)
                                .comment("Great!")
                                .type(ReviewType.RENTER_TO_OWNER)
                                .build();

                when(reviewRepository.saveAndFlush(any(Review.class))).thenReturn(savedReview);

                // Mock behavior for updateRatings -> updateUserReputation
                when(reviewRepository.findByOwnerId(owner.getId())).thenReturn(List.of(savedReview));
                when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

                ReviewResponse response = reviewService.createReview(request);

                assertThat(response).isNotNull();
                assertThat(response.getRating()).isEqualTo(5);
                assertThat(response.getComment()).isEqualTo("Great!");
                verify(reviewRepository).saveAndFlush(any(Review.class));

                // Verify reputation update
                verify(userRepository).save(owner);
                assertThat(owner.getReputationScore()).isEqualTo(5.0);
        }

        @Test
        void createReview_bookingNotCompleted_throwsException() {
                booking.setStatus(BookingStatus.APPROVED);
                CreateReviewRequest request = new CreateReviewRequest(booking.getId(), ReviewType.RENTER_TO_OWNER, 5,
                                "Great!");

                when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

                assertThatThrownBy(() -> reviewService.createReview(request))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessage("Booking must be completed to leave a review");
        }

        @Test
        void createReview_reviewAlreadyExists_throwsException() {
                CreateReviewRequest request = new CreateReviewRequest(booking.getId(), ReviewType.RENTER_TO_OWNER, 5,
                                "Great!");

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

                when(reviewRepository.saveAndFlush(any(Review.class))).thenReturn(updatedReview);

                // Mock behavior for updateRatings -> updateUserReputation
                when(reviewRepository.findByOwnerId(owner.getId())).thenReturn(List.of(updatedReview));
                when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

                ReviewResponse response = reviewService.updateReview(reviewId, request);

                assertThat(response).isNotNull();
                assertThat(response.getRating()).isEqualTo(5);
                assertThat(response.getComment()).isEqualTo("Updated comment");
                verify(reviewRepository).findById(reviewId);
                verify(reviewRepository).saveAndFlush(any(Review.class));

                // Verify reputation update
                verify(userRepository).save(owner);
                assertThat(owner.getReputationScore()).isEqualTo(5.0);
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

        @Test
        void createReview_toolReview_returnsResponse() {
                CreateReviewRequest request = new CreateReviewRequest(booking.getId(), ReviewType.RENTER_TO_TOOL, 5,
                                "Excellent tool!");

                when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

                Review savedReview = Review.builder()
                                .id(UUID.randomUUID())
                                .booking(booking)
                                .reviewer(renter)
                                .owner(null)
                                .tool(tool)
                                .rating(5)
                                .comment("Excellent tool!")
                                .type(ReviewType.RENTER_TO_TOOL)
                                .build();

                when(reviewRepository.saveAndFlush(any(Review.class))).thenReturn(savedReview);

                // Mock behavior for updateRatings -> updateToolRating
                when(reviewRepository.findByToolId(tool.getId())).thenReturn(List.of(savedReview));
                when(toolRepository.findById(tool.getId())).thenReturn(Optional.of(tool));

                ReviewResponse response = reviewService.createReview(request);

                assertThat(response).isNotNull();
                assertThat(response.getRating()).isEqualTo(5);
                assertThat(response.getComment()).isEqualTo("Excellent tool!");
                assertThat(response.getToolId()).isEqualTo(tool.getId());
                verify(reviewRepository).saveAndFlush(any(Review.class));

                // Verify tool rating update
                verify(toolRepository).save(tool);
                assertThat(tool.getOverallRating()).isEqualTo(5.0);
                assertThat(tool.getNumRatings()).isEqualTo(1);
        }

        @Test
        void createReview_toolReviewAlreadyExists_throwsException() {
                CreateReviewRequest request = new CreateReviewRequest(booking.getId(), ReviewType.RENTER_TO_TOOL, 5,
                                "Great tool!");

                Review existingReview = Review.builder().type(ReviewType.RENTER_TO_TOOL).build();
                booking.setReviews(List.of(existingReview));

                when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

                assertThatThrownBy(() -> reviewService.createReview(request))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessage("Review of this type already exists for this booking");
        }

        @Test
        void testCreateReview_SingleOneStar_UpdatesReputationCorrectly() {
                // Arrange
                CreateReviewRequest request = new CreateReviewRequest();
                request.setBookingId(booking.getId());
                request.setRating(1);
                request.setComment("Bad");
                request.setType(ReviewType.RENTER_TO_OWNER);

                when(bookingRepository.findById(booking.getId())).thenReturn(java.util.Optional.of(booking));

                // Mock the findByOwnerId to return the list INCLUDING the new review
                Review reviewWithRating1 = Review.builder().rating(1).build();
                when(reviewRepository.findByOwnerId(any(UUID.class)))
                                .thenReturn(java.util.List.of(reviewWithRating1));

                when(reviewRepository.saveAndFlush(any(Review.class)))
                                .thenAnswer(invocation -> {
                                        Review r = invocation.getArgument(0);
                                        Review saved = Review.builder()
                                                        .id(UUID.randomUUID())
                                                        .booking(r.getBooking())
                                                        .reviewer(r.getReviewer())
                                                        .owner(r.getOwner())
                                                        .tool(r.getTool())
                                                        .rating(r.getRating())
                                                        .comment(r.getComment())
                                                        .type(r.getType())
                                                        .build();
                                        return saved;
                                });

                when(userRepository.findById(any(UUID.class)))
                                .thenReturn(java.util.Optional.of(owner));

                // Act
                reviewService.createReview(request);

                // Assert
                // Verify user reputation was set to 1.0
                verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(u -> u.getReputationScore() == 1.0));
        }

        @Test
        void testRecalculateAllReputations() {
                // Arrange
                User user1 = User.builder().id(UUID.randomUUID()).firstName("U1").lastName("L1").build();
                User user2 = User.builder().id(UUID.randomUUID()).firstName("U2").lastName("L2").build();
                List<User> users = List.of(user1, user2);

                when(userRepository.findAll()).thenReturn(users);

                // Mock behavior for updates
                // findByOwnerId might return empty list or some reviews, simple empty check is
                // enough
                // to verify flow calls the right methods
                when(reviewRepository.findByOwnerId(any(UUID.class))).thenReturn(java.util.Collections.emptyList());

                // Act
                reviewService.recalculateAllReputations();

                // Assert
                verify(userRepository).findAll();
                // Should find reviews for both users
                verify(reviewRepository, org.mockito.Mockito.times(2)).findByOwnerId(any(UUID.class));
        }
}
