package com.toolshed.backend.repository;

import com.toolshed.backend.repository.entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByToolId(UUID toolId);
    List<Review> findByOwnerId(UUID ownerId);
}
