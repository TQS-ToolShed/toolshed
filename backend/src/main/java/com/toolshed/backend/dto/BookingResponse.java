package com.toolshed.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.ConditionStatus;
import com.toolshed.backend.repository.enums.DepositStatus;
import com.toolshed.backend.repository.enums.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private UUID id;
    private UUID toolId;
    private UUID renterId;
    private UUID ownerId;
    private String ownerName;
    private String toolTitle;
    private LocalDate startDate;
    private LocalDate endDate;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private Double totalPrice;
    private ReviewResponse review;
    private ReviewResponse ownerReview;
    private ReviewResponse toolReview;

    // Condition Report Fields
    private ConditionStatus conditionStatus;
    private String conditionDescription;
    private LocalDateTime conditionReportedAt;
    private String conditionReportedByName;

    // Deposit Fields
    private DepositStatus depositStatus;
    private Double depositAmount;
    private LocalDateTime depositPaidAt;
}
