package com.toolshed.backend.repository;

import com.toolshed.backend.repository.entities.Tool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ToolRepository extends JpaRepository<Tool, UUID> {
    List<Tool> findByOwnerId(UUID ownerId);
    List<Tool> findByActiveTrue();

    @Query("SELECT t FROM Tool t WHERE t.active = true AND t.location LIKE %:location%")
    List<Tool> searchByLocation(String location);
}
