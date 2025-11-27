package com.toolshed.backend.repository.entities;

import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.DamageResolutionStatus;
import com.toolshed.backend.repository.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "booking")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tool_id", nullable = false)
    private Tool tool;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "renter_id", nullable = false)
    private User renter;

    // Denormalized reference to owner for easier queries/history
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private Double totalPrice;

    // Damage Tracking
    private boolean toolReturnedDamaged;
    private boolean damageReportedByRenter;
    private boolean damageReportedByOwner;
    
    @Column(length = 1000)
    private String damageDescription;

    @Enumerated(EnumType.STRING)
    private DamageResolutionStatus damageResolutionStatus;
}