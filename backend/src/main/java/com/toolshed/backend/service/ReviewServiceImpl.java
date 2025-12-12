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

    public ReviewServiceImpl(ReviewRepository reviewRepository, BookingRepository bookingRepository) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
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

        Review savedReview = reviewRepository.save(review);

        return ReviewResponse.builder()
                .id(savedReview.getId())
                .bookingId(savedReview.getBooking().getId())
                .reviewerId(savedReview.getReviewer().getId())
                .reviewerName(savedReview.getReviewer().getFirstName() + " " + savedReview.getReviewer().getLastName())
                .ownerId(savedReview.getOwner().getId())
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

        Review savedReview = reviewRepository.save(review);

        return ReviewResponse.builder()
                .id(savedReview.getId())
                .bookingId(savedReview.getBooking().getId())
                .reviewerId(savedReview.getReviewer().getId())
                .reviewerName(savedReview.getReviewer().getFirstName() + " " + savedReview.getReviewer().getLastName())
                .ownerId(savedReview.getOwner().getId())
                .toolId(savedReview.getTool().getId())
                .rating(savedReview.getRating())
                .comment(savedReview.getComment())
                .date(savedReview.getDate())
                .build();
    }
}
