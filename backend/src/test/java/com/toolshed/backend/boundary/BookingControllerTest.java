package com.toolshed.backend.boundary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolshed.backend.dto.BookingResponse;
import com.toolshed.backend.dto.CreateBookingRequest;
import com.toolshed.backend.dto.OwnerBookingResponse;
import com.toolshed.backend.repository.enums.BookingStatus;
import com.toolshed.backend.repository.enums.PaymentStatus;
import com.toolshed.backend.service.BookingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Test
    @DisplayName("Should list bookings for an owner")
    void listOwnerBookings() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        LocalDate start = LocalDate.now().plusDays(2);
        LocalDate end = start.plusDays(1);

        OwnerBookingResponse booking = OwnerBookingResponse.builder()
                .id(bookingId)
                .toolId(UUID.randomUUID())
                .toolTitle("Cordless Drill")
                .renterId(UUID.randomUUID())
                .renterName("Jamie Renter")
                .startDate(start)
                .endDate(end)
                .status(BookingStatus.PENDING)
                .totalPrice(45.0)
                .build();

        when(bookingService.getBookingsForOwner(ownerId)).thenReturn(List.of(booking));

        mockMvc.perform(get("/api/bookings").param("ownerId", ownerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(bookingId.toString())));
    }

    @Test
    @DisplayName("Should update booking status for approve/reject")
    void updateBookingStatus() throws Exception {
        UUID bookingId = UUID.randomUUID();
        BookingResponse response = BookingResponse.builder()
                .id(bookingId)
                .toolId(UUID.randomUUID())
                .renterId(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .startDate(LocalDate.now().plusDays(3))
                .endDate(LocalDate.now().plusDays(4))
                .status(BookingStatus.APPROVED)
                .paymentStatus(PaymentStatus.PENDING)
                .totalPrice(30.0)
                .build();

        when(bookingService.updateBookingStatus(eq(bookingId), eq(BookingStatus.APPROVED))).thenReturn(response);

        mockMvc.perform(put("/api/bookings/{bookingId}/status", bookingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(bookingId.toString())))
                .andExpect(jsonPath("$.status", is("APPROVED")));
    }

    @Test
    @DisplayName("Should return bad request when service rejects status change")
    void updateBookingStatusBadRequest() throws Exception {
        UUID bookingId = UUID.randomUUID();
        when(bookingService.updateBookingStatus(eq(bookingId), eq(BookingStatus.REJECTED)))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Booking decision is already final"));

        mockMvc.perform(put("/api/bookings/{bookingId}/status", bookingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isBadRequest());
    }
}
