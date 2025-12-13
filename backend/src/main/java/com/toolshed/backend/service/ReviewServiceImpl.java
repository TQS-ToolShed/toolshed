package com.toolshed.backend.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toolshed.backend.dto.CreateReviewRequest;
import com.toolshed.backend.dto.ReviewResponse;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ReviewRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.entities.Review;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.ReviewType;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final com.toolshed.backend.repository.UserRepository userRepository;
    private final com.toolshed.backend.repository.ToolRepository toolRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository, BookingRepository bookingRepository,
            com.toolshed.backend.repository.UserRepository userRepository,
            com.toolshed.backend.repository.ToolRepository toolRepository) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.toolRepository = toolRepository;
    }

    @Override
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalStateException("Booking must be completed to leave a review");
        }

        ReviewType type = request.getType() != null ? request.getType() : ReviewType.RENTER_TO_OWNER;

        boolean exists = booking.getReviews() != null && booking.getReviews().stream()
                .anyMatch(r -> r.getType() == type || (type == ReviewType.RENTER_TO_OWNER && r.getType() == null));

        if (exists) {
            throw new IllegalStateException("Review of this type already exists for this booking");
        }

        User reviewer;
        User target;

        if (type == ReviewType.OWNER_TO_RENTER) {
            reviewer = booking.getOwner();
            target = booking.getRenter();
        } else if (type == ReviewType.RENTER_TO_TOOL) {
            reviewer = booking.getRenter();
            target = booking.getOwner(); // Set owner so it counts towards reputation
        } else {
            reviewer = booking.getRenter();
            target = booking.getOwner();
        }

        Review review = Review.builder()
                .booking(booking)
                .reviewer(reviewer)
                .owner(target)
                .tool(booking.getTool())
                .rating(request.getRating())
                .comment(request.getComment())
                .type(type)
                .build();

        Review savedReview = reviewRepository.saveAndFlush(review);

        updateRatings(savedReview);

        return ReviewResponse.builder()
                .id(savedReview.getId())
                .bookingId(savedReview.getBooking().getId())
                .reviewerId(savedReview.getReviewer().getId())
                .reviewerName(savedReview.getReviewer().getFirstName() + " " + savedReview.getReviewer().getLastName())
                .ownerId(savedReview.getOwner() != null ? savedReview.getOwner().getId() : null)
                .toolId(savedReview.getTool().getId())
                .rating(savedReview.getRating())
                .comment(savedReview.getComment())
                .date(savedReview.getDate())
                .build();
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(UUID reviewId, CreateReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review savedReview = reviewRepository.saveAndFlush(review);

        updateRatings(savedReview);

        return ReviewResponse.builder()
                .id(savedReview.getId())
                .bookingId(savedReview.getBooking().getId())
                .reviewerId(savedReview.getReviewer().getId())
                .reviewerName(savedReview.getReviewer().getFirstName() + " " + savedReview.getReviewer().getLastName())
                .ownerId(savedReview.getOwner() != null ? savedReview.getOwner().getId() : null)
                .toolId(savedReview.getTool().getId())
                .rating(savedReview.getRating())
                .comment(savedReview.getComment())
                .date(savedReview.getDate())
                .build();
    }

    @Override
    @Transactional
    public void recalculateAllReputations() {
        java.util.List<User> users = userRepository.findAll();
        for (User user : users) {
            updateUserReputation(user.getId());
        }
    }

    private void updateRatings(Review review) {
        if (review.getType() == ReviewType.RENTER_TO_TOOL) {
            updateToolRating(review.getTool().getId());
        }

        // Update the target user's reputation (owner or renter)
        if (review.getOwner() != null) {
            updateUserReputation(review.getOwner().getId());
        }
    }

    private void updateToolRating(UUID toolId) {
        java.util.List<Review> reviews = reviewRepository.findByToolId(toolId).stream()
                .filter(r -> r.getType() == ReviewType.RENTER_TO_TOOL)
                .collect(java.util.stream.Collectors.toList());

        if (reviews.isEmpty()) {
            return;
        }

        double average = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        // Round to 1 decimal place
        average = Math.round(average * 10.0) / 10.0;

        com.toolshed.backend.repository.entities.Tool tool = toolRepository.findById(toolId)
                .orElseThrow(() -> new IllegalStateException("Tool not found during rating update"));

        tool.setOverallRating(average);
        tool.setNumRatings(reviews.size());

        toolRepository.save(tool);
    }

    private void updateUserReputation(UUID userId) {
        // Find reviews where this user is the TARGET (owner field in Review entity)
        // Note: ReviewRepository.findByOwnerId finds by the 'owner' field, which is the
        // target user
        java.util.List<Review> reviews = reviewRepository.findByOwnerId(userId);

        if (reviews.isEmpty()) {
            return;
        }

        double average = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        // Round to 1 decimal place
        average = Math.round(average * 10.0) / 10.0;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found during reputation update"));

        user.setReputationScore(average);

        userRepository.save(user);
    }
}
