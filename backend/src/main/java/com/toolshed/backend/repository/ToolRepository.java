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
    List<Tool> findByTitle(String title);

    @Query("SELECT t FROM Tool t WHERE t.active = true " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "    (LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
           "AND (:location IS NULL OR :location = '' OR " +
           "    (LOWER(t.district) LIKE LOWER(CONCAT('%', :location, '%')) OR " +
           "     LOWER(t.municipality) LIKE LOWER(CONCAT('%', :location, '%')))) " +
           "AND (:minPrice IS NULL OR t.pricePerDay >= :minPrice) " +
           "AND (:maxPrice IS NULL OR t.pricePerDay <= :maxPrice)")
    List<Tool> searchTools(@Param("keyword") String keyword, 
                           @Param("location") String location,
                           @Param("minPrice") Double minPrice,
                           @Param("maxPrice") Double maxPrice);

}
