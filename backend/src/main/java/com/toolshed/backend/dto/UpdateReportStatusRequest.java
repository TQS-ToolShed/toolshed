package com.toolshed.backend.dto;

import com.toolshed.backend.repository.enums.ReportStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReportStatusRequest {
    @NotNull
    private ReportStatus status;
}
