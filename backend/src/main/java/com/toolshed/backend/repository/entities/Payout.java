package com.toolshed.backend.repository.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.toolshed.backend.repository.enums.PayoutStatus;

import jakarta.persistence.Column;
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

/**
 * Entity representing a payout request from an owner.
 */
@Entity
@Table(name = "payout")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayoutStatus status;

    /**
     * Simulated Stripe Transfer ID.
     */
    private String stripeTransferId;

    @CreationTimestamp
    private LocalDateTime requestedAt;

    private LocalDateTime completedAt;

    /**
     * Description for wallet history display.
     * For cancellation income: "Cancellation fee from [renter name]"
     */
    private String description;

    /**
     * True if this is income (e.g., cancellation fee), false if payout.
     */
    @Builder.Default
    private Boolean isIncome = false;
}
