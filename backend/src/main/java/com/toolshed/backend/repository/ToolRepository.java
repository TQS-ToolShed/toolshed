package com.toolshed.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.toolshed.backend.repository.entities.Tool;

@Repository
public interface ToolRepository extends JpaRepository<Tool, UUID> {
    List<Tool> findByOwnerId(UUID ownerId);
    List<Tool> findByActiveTrue();

    /**
     * Unified Search: Handles Keyword AND/OR Location filtering.
     * * Logic:
     * 1. Tool must be ACTIVE.
     * 2. AND: If keyword is provided, match Title OR Description.
     * 3. AND: If location is provided, match Location.
     */
    @Query("SELECT t FROM Tool t WHERE t.active = true " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "    (LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
           "AND (:location IS NULL OR :location = '' OR " +
           "    LOWER(t.location) LIKE LOWER(CONCAT('%', :location, '%')))")
    List<Tool> searchTools(@Param("keyword") String keyword, 
                           @Param("location") String location);
}
