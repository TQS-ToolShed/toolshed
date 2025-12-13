package com.toolshed.backend.service;

import java.util.UUID;

import com.toolshed.backend.dto.CreateReviewRequest;
import com.toolshed.backend.dto.ReviewResponse;

public interface ReviewService {
    ReviewResponse createReview(CreateReviewRequest request);
    ReviewResponse updateReview(UUID reviewId, CreateReviewRequest request);
}
