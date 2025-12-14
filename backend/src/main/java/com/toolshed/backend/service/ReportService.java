package com.toolshed.backend.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.toolshed.backend.dto.CreateReportRequest;
import com.toolshed.backend.dto.ReportResponse;
import com.toolshed.backend.dto.UpdateReportStatusRequest;
import com.toolshed.backend.repository.BookingRepository;
import com.toolshed.backend.repository.ReportRepository;
import com.toolshed.backend.repository.ToolRepository;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.Booking;
import com.toolshed.backend.repository.entities.Report;
import com.toolshed.backend.repository.entities.Tool;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.ReportStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ToolRepository toolRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public ReportResponse createReport(CreateReportRequest request) {
        User reporter = userRepository.findById(request.getReporterId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporter not found"));

        Tool tool = null;
        Booking booking = null;

        if (request.getToolId() != null) {
            tool = toolRepository.findById(request.getToolId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tool not found"));
        }
        if (request.getBookingId() != null) {
            booking = bookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        }

        Report report = Report.builder()
                .reporter(reporter)
                .tool(tool)
                .booking(booking)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(ReportStatus.OPEN)
                .build();

        Report saved = reportRepository.save(report);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getAllReports(ReportStatus status) {
        List<Report> reports = status == null ? reportRepository.findAll() : reportRepository.findByStatus(status);
        return reports.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ReportResponse updateStatus(UUID reportId, UpdateReportStatusRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found"));
        report.setStatus(request.getStatus());
        Report saved = reportRepository.save(report);
        return toResponse(saved);
    }

    @Transactional
    public void deleteReport(UUID reportId) {
        if (!reportRepository.existsById(reportId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found");
        }
        reportRepository.deleteById(reportId);
    }

    private ReportResponse toResponse(Report report) {
        User resolvedReporter = resolveUser(report.getReporter());
        return ReportResponse.builder()
                .id(report.getId())
                .reporterId(resolvedReporter != null ? resolvedReporter.getId() : null)
                .reporterEmail(resolvedReporter != null ? resolvedReporter.getEmail() : null)
                .toolId(report.getTool() != null ? report.getTool().getId() : null)
                .toolTitle(report.getTool() != null ? report.getTool().getTitle() : null)
                .bookingId(report.getBooking() != null ? report.getBooking().getId() : null)
                .title(report.getTitle())
                .description(report.getDescription())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }

    private User resolveUser(User user) {
        if (user == null) {
            return null;
        }
        UUID id = user.getId();
        if (id == null) {
            return user;
        }
        return userRepository.findById(id).orElse(user);
    }
}
