package com.toolshed.backend.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.toolshed.backend.repository.enums.BookingStatus;
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
    private String toolTitle;
    private LocalDate startDate;
    private LocalDate endDate;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private Double totalPrice;
}
