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

    @Query("SELECT t FROM Tool t WHERE t.active = true AND t.location LIKE %:location%")
    List<Tool> searchByLocation(String location);

    /**
     * Searches for active tools where the title or description contains the keyword.
     * Case-insensitive.
     *
     * @param keyword The search term provided by the user
     * @return A list of matching Tools, or an empty list if none found.
     */
    @Query("SELECT t FROM Tool t WHERE t.active = true AND " +
           "(LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Tool> searchTools(@Param("keyword") String keyword);
}
