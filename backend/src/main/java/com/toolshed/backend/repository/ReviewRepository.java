package com.toolshed.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.toolshed.backend.repository.entities.Review;
import com.toolshed.backend.repository.enums.ReviewType;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByToolId(UUID toolId);
    List<Review> findByOwnerId(UUID ownerId);
    boolean existsByBookingId(UUID bookingId);
    boolean existsByBookingIdAndType(UUID bookingId, ReviewType type);
}
