package com.toolshed.backend.boundary;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolshed.backend.dto.CreateReportRequest;
import com.toolshed.backend.dto.ReportResponse;
import com.toolshed.backend.dto.UpdateReportStatusRequest;
import com.toolshed.backend.repository.enums.ReportStatus;
import com.toolshed.backend.service.ReportService;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReportService reportService;

    private ReportResponse sampleResponse() {
        return ReportResponse.builder()
                .id(UUID.randomUUID())
                .reporterId(UUID.randomUUID())
                .reporterEmail("reporter@example.com")
                .toolId(UUID.randomUUID())
                .bookingId(UUID.randomUUID())
                .title("Issue")
                .description("Something happened")
                .status(ReportStatus.OPEN)
                .build();
    }

    @Test
    @DisplayName("POST /api/reports creates a report")
    void createReport() throws Exception {
        CreateReportRequest req = CreateReportRequest.builder()
                .reporterId(UUID.randomUUID())
                .bookingId(UUID.randomUUID())
                .toolId(UUID.randomUUID())
                .title("Problem")
                .description("Details")
                .build();
        ReportResponse resp = sampleResponse();
        when(reportService.createReport(any(CreateReportRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/reports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Issue")));

        verify(reportService).createReport(any(CreateReportRequest.class));
    }

    @Test
    @DisplayName("GET /api/reports lists reports")
    void listReports() throws Exception {
        ReportResponse resp = sampleResponse();
        when(reportService.getAllReports(null)).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/reports").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(reportService).getAllReports(null);
    }

    @Test
    @DisplayName("PUT /api/reports/{id}/status updates status")
    void updateStatus() throws Exception {
        ReportResponse resp = sampleResponse();
        resp.setStatus(ReportStatus.RESOLVED);
        UUID id = resp.getId();
        when(reportService.updateStatus(any(UUID.class), any(UpdateReportStatusRequest.class)))
                .thenReturn(resp);

        UpdateReportStatusRequest req = UpdateReportStatusRequest.builder()
                .status(ReportStatus.RESOLVED)
                .build();

        mockMvc.perform(put("/api/reports/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RESOLVED")));

        verify(reportService).updateStatus(eq(id), any(UpdateReportStatusRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/reports/{id} removes report")
    void deleteReport() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/reports/{id}", id)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(reportService).deleteReport(id);
    }
}
