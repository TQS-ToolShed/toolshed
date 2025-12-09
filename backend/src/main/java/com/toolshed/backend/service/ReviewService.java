package com.toolshed.backend.service;

import com.toolshed.backend.dto.CreateReviewRequest;
import com.toolshed.backend.dto.ReviewResponse;

public interface ReviewService {
    ReviewResponse createReview(CreateReviewRequest request);
}
