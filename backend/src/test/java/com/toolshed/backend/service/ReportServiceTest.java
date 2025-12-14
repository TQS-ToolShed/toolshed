package com.toolshed.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
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

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ToolRepository toolRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private ReportService reportService;

    private User sampleUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("reporter@example.com");
        user.setPassword("secret");
        user.setReputationScore(0.0);
        return user;
    }

    private Tool sampleTool() {
        Tool tool = new Tool();
        tool.setId(UUID.randomUUID());
        tool.setTitle("Test Tool");
        tool.setPricePerDay(10.0);
        tool.setDistrict("Aveiro");
        tool.setOwner(sampleUser());
        tool.setActive(true);
        tool.setOverallRating(0.0);
        tool.setNumRatings(0);
        return tool;
    }

    private Booking sampleBooking(User renter, Tool tool) {
        Booking booking = new Booking();
        booking.setId(UUID.randomUUID());
        booking.setRenter(renter);
        booking.setOwner(tool.getOwner());
        booking.setTool(tool);
        booking.setTotalPrice(30.0);
        return booking;
    }

    @Test
    @DisplayName("createReport should persist and return mapped response")
    void createReport_persists() {
        User reporter = sampleUser();
        Tool tool = sampleTool();
        Booking booking = sampleBooking(reporter, tool);

        CreateReportRequest req = CreateReportRequest.builder()
                .reporterId(reporter.getId())
                .toolId(tool.getId())
                .bookingId(booking.getId())
                .title("Broken tool")
                .description("The tool was broken on arrival")
                .build();

        when(userRepository.findById(reporter.getId())).thenReturn(Optional.of(reporter));
        when(toolRepository.findById(tool.getId())).thenReturn(Optional.of(tool));
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> {
            Report r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        ReportResponse response = reportService.createReport(req);

        assertThat(response.getTitle()).isEqualTo("Broken tool");
        assertThat(response.getReporterId()).isEqualTo(reporter.getId());
        assertThat(response.getToolId()).isEqualTo(tool.getId());
        assertThat(response.getBookingId()).isEqualTo(booking.getId());
        assertThat(response.getStatus()).isEqualTo(ReportStatus.OPEN);

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("The tool was broken on arrival");
    }

    @Test
    @DisplayName("createReport throws when reporter missing")
    void createReport_missingReporter() {
        CreateReportRequest req = CreateReportRequest.builder()
                .reporterId(UUID.randomUUID())
                .title("Issue")
                .description("desc")
                .build();

        when(userRepository.findById(req.getReporterId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.createReport(req))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("getAllReports filters by status when provided")
    void getAllReports_filtersByStatus() {
        Report openReport = Report.builder().id(UUID.randomUUID()).status(ReportStatus.OPEN).build();
        Report resolvedReport = Report.builder().id(UUID.randomUUID()).status(ReportStatus.RESOLVED).build();

        when(reportRepository.findByStatus(ReportStatus.OPEN)).thenReturn(List.of(openReport));

        List<ReportResponse> responses = reportService.getAllReports(ReportStatus.OPEN);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus()).isEqualTo(ReportStatus.OPEN);
        verify(reportRepository, never()).findAll();
    }

    @Test
    @DisplayName("updateStatus updates and returns report")
    void updateStatus_updates() {
        Report report = Report.builder().id(UUID.randomUUID()).status(ReportStatus.OPEN).build();
        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));

        ReportResponse resp = reportService.updateStatus(report.getId(),
                UpdateReportStatusRequest.builder().status(ReportStatus.RESOLVED).build());

        assertThat(resp.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        verify(reportRepository).save(report);
    }

    @Test
    @DisplayName("deleteReport removes report when exists")
    void deleteReport_exists() {
        UUID id = UUID.randomUUID();
        when(reportRepository.existsById(id)).thenReturn(true);

        reportService.deleteReport(id);

        verify(reportRepository).deleteById(id);
    }

    @Test
    @DisplayName("deleteReport throws 404 when missing")
    void deleteReport_missing() {
        UUID id = UUID.randomUUID();
        when(reportRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> reportService.deleteReport(id))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(reportRepository, never()).deleteById(id);
    }
}
