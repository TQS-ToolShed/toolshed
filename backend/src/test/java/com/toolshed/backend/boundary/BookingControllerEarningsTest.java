package com.toolshed.backend.boundary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolshed.backend.dto.MonthlyEarnings;
import com.toolshed.backend.dto.OwnerEarningsResponse;
import com.toolshed.backend.service.BookingService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
class BookingControllerEarningsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @Test
    @DisplayName("GET /api/bookings/earnings should return owner earnings")
    void getOwnerEarnings_shouldReturnEarnings() throws Exception {
        // Arrange
        UUID ownerId = UUID.randomUUID();
        
        MonthlyEarnings feb2024 = MonthlyEarnings.builder()
                .year(2024)
                .month(2)
                .totalEarnings(30.0)
                .bookingCount(1L)
                .build();

        MonthlyEarnings jan2024 = MonthlyEarnings.builder()
                .year(2024)
                .month(1)
                .totalEarnings(60.0)
                .bookingCount(2L)
                .build();

        OwnerEarningsResponse response = OwnerEarningsResponse.builder()
                .monthlyEarnings(Arrays.asList(feb2024, jan2024))
                .totalEarnings(90.0)
                .build();

        when(bookingService.getOwnerEarnings(ownerId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/bookings/earnings")
                        .param("ownerId", ownerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEarnings").value(90.0))
                .andExpect(jsonPath("$.monthlyEarnings").isArray())
                .andExpect(jsonPath("$.monthlyEarnings.length()").value(2))
                .andExpect(jsonPath("$.monthlyEarnings[0].year").value(2024))
                .andExpect(jsonPath("$.monthlyEarnings[0].month").value(2))
                .andExpect(jsonPath("$.monthlyEarnings[0].totalEarnings").value(30.0))
                .andExpect(jsonPath("$.monthlyEarnings[0].bookingCount").value(1))
                .andExpect(jsonPath("$.monthlyEarnings[1].year").value(2024))
                .andExpect(jsonPath("$.monthlyEarnings[1].month").value(1))
                .andExpect(jsonPath("$.monthlyEarnings[1].totalEarnings").value(60.0))
                .andExpect(jsonPath("$.monthlyEarnings[1].bookingCount").value(2));
    }

    @Test
    @DisplayName("GET /api/bookings/earnings should return empty earnings when no bookings")
    void getOwnerEarnings_shouldReturnEmptyWhenNoBookings() throws Exception {
        // Arrange
        UUID ownerId = UUID.randomUUID();

        OwnerEarningsResponse response = OwnerEarningsResponse.builder()
                .monthlyEarnings(Arrays.asList())
                .totalEarnings(0.0)
                .build();

        when(bookingService.getOwnerEarnings(ownerId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/bookings/earnings")
                        .param("ownerId", ownerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEarnings").value(0.0))
                .andExpect(jsonPath("$.monthlyEarnings").isEmpty());
    }
}
