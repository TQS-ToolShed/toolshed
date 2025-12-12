package com.toolshed.backend.repository.entities;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.PaymentStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @jakarta.persistence.OneToMany(mappedBy = "booking", fetch = FetchType.LAZY)
    private List<Review> reviews;

    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private Double totalPrice;

    // Damage tracking has been moved to ToolDamage entity
}