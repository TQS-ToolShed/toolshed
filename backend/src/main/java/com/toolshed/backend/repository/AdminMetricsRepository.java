package com.toolshed.backend.repository;

import com.toolshed.backend.repository.entities.AdminMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface AdminMetricsRepository extends JpaRepository<AdminMetrics, UUID> {
    AdminMetrics findByDate(LocalDate date);
}
