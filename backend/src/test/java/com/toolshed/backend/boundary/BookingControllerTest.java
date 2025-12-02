package com.toolshed.backend.boundary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolshed.backend.dto.BookingResponse;
import com.toolshed.backend.dto.CreateBookingRequest;
import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.PaymentStatus;
import com.toolshed.backend.service.BookingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingService bookingService;

    @Test
    @DisplayName("Should create booking and return response body")
    void createBooking() throws Exception {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(1);
        BookingResponse response = BookingResponse.builder()
                .id(UUID.randomUUID())
                .toolId(UUID.randomUUID())
                .renterId(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .startDate(start)
                .endDate(end)
                .status(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .totalPrice(20.0)
                .build();

        when(bookingService.createBooking(any())).thenReturn(response);

        CreateBookingRequest request = CreateBookingRequest.builder()
                .toolId(response.getToolId())
                .renterId(response.getRenterId())
                .startDate(start)
                .endDate(end)
                .build();

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(response.getId().toString())))
                .andExpect(jsonPath("$.totalPrice", is(response.getTotalPrice())))
                .andExpect(jsonPath("$.status", is(response.getStatus().name())));
    }

    @Test
    @DisplayName("Should reject past dates at the controller validation layer")
    void rejectPastDates() throws Exception {
        LocalDate start = LocalDate.now().minusDays(1);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .toolId(UUID.randomUUID())
                .renterId(UUID.randomUUID())
                .startDate(start)
                .endDate(LocalDate.now().plusDays(1))
                .build();

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
