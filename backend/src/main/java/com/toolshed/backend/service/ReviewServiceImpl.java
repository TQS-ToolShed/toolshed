package com.toolshed.backend.service;

import com.toolshed.backend.dto.CreateReviewRequest;
import com.toolshed.backend.dto.ReviewResponse;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ReviewRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.entities.Review;
import com.toolshed.backend.repository.enums.BookingStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        if (reviewRepository.existsByBookingId(request.getBookingId())) {
            throw new IllegalStateException("Review already exists for this booking");
        }

        Review review = Review.builder()
                .booking(booking)
                .reviewer(booking.getRenter())
                .owner(booking.getOwner())
                .tool(booking.getTool())
                .rating(request.getRating())
                .comment(request.getComment())
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
}
