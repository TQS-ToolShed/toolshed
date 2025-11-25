package com.toolshed.backend.repository;

import com.toolshed.backend.repository.entities.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByRenterId(UUID renterId);
    List<Booking> findByOwnerId(UUID ownerId);
    List<Booking> findByToolId(UUID toolId);

    @Query("SELECT b FROM Booking b WHERE b.tool.id = :toolId AND b.status NOT IN ('CANCELLED', 'REJECTED') AND ((b.startDate <= :endDate) AND (b.endDate >= :startDate))")
    List<Booking> findOverlappingBookings(UUID toolId, LocalDate startDate, LocalDate endDate);
}
