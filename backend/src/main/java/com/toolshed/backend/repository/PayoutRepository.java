package com.toolshed.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.toolshed.backend.repository.entities.Payout;

/**
 * Repository for Payout entity operations.
 */
@Repository
public interface PayoutRepository extends JpaRepository<Payout, UUID> {

    /**
     * Find all payouts for an owner, ordered by request date descending.
     */
    List<Payout> findByOwnerIdOrderByRequestedAtDesc(UUID ownerId);
}
