package com.toolshed.backend.repository.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tool")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tool {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;
    
    @Column(length = 1000)
    private String description;
    
    private Double pricePerDay;
    private String location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    private boolean active;

    // Storing as String (e.g., JSON or simplified text) as per schema prompt
    // In a real app, this might be a separate element collection or table
    private String availabilityCalendar; 

    private Double overallRating;

    private boolean isDamaged;
    
    @Column(length = 1000)
    private String damageDescription;
    
    private LocalDateTime damageReportedDate;
}