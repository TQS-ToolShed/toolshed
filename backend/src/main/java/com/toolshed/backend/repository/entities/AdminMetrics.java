package com.toolshed.backend.repository.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "admin_metrics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private LocalDate date;
    
    private Integer activeUsersCount;
    private Integer activeListingsCount;
    private Integer bookingsCount;

    @Column(columnDefinition = "TEXT") 
    private String otherStats; // Assuming JSON storage for flexibility
}