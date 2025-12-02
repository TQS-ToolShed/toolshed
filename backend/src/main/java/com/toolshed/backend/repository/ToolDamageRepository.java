package com.toolshed.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.toolshed.backend.repository.entities.ToolDamage;

@Repository
public interface ToolDamageRepository extends JpaRepository<ToolDamage, UUID> {
    
    List<ToolDamage> findByToolId(UUID toolId);
    
    List<ToolDamage> findByToolIdAndResolvedFalse(UUID toolId);
    
    List<ToolDamage> findByBookingId(UUID bookingId);
}
