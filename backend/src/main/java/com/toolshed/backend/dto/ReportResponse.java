package com.toolshed.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.toolshed.backend.repository.enums.ReportStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private UUID id;
    private UUID reporterId;
    private String reporterEmail;
    private UUID toolId;
    private String toolTitle;
    private UUID bookingId;
    private String title;
    private String description;
    private ReportStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
