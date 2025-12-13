package com.toolshed.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.toolshed.backend.service.ReviewService;

@Component
public class ReputationInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ReputationInitializer.class);

    private final ReviewService reviewService;

    public ReputationInitializer(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Recalculating reputation scores for all users...");
        reviewService.recalculateAllReputations();
        logger.info("Reputation score recalculation completed.");
    }
}
