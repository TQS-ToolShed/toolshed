package com.toolshed.backend.boundary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toolshed.backend.dto.BookingResponse;
import com.toolshed.backend.dto.CancelBookingResponse;
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
        @DisplayName("Should list bookings for a renter")
        void listRenterBookings() throws Exception {
                UUID renterId = UUID.randomUUID();
                BookingResponse booking = BookingResponse.builder()
                                .id(UUID.randomUUID())
                                .toolId(UUID.randomUUID())
                                .renterId(renterId)
                                .ownerId(UUID.randomUUID())
                                .toolTitle("Angle Grinder")
                                .startDate(LocalDate.now().plusDays(5))
                                .endDate(LocalDate.now().plusDays(6))
                                .status(BookingStatus.APPROVED)
                                .paymentStatus(PaymentStatus.PENDING)
                                .totalPrice(25.0)
                                .build();

                when(bookingService.getBookingsForRenter(renterId)).thenReturn(List.of(booking));

                mockMvc.perform(get("/api/bookings").param("renterId", renterId.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].toolTitle", is("Angle Grinder")));
        }

        @Test
        @DisplayName("Should list bookings for a tool")
        void listToolBookings() throws Exception {
                UUID toolId = UUID.randomUUID();
                BookingResponse booking = BookingResponse.builder()
                                .id(UUID.randomUUID())
                                .toolId(toolId)
                                .renterId(UUID.randomUUID())
                                .ownerId(UUID.randomUUID())
                                .toolTitle("Hammer Drill")
                                .startDate(LocalDate.now().plusDays(7))
                                .endDate(LocalDate.now().plusDays(8))
                                .status(BookingStatus.APPROVED)
                                .paymentStatus(PaymentStatus.PENDING)
                                .totalPrice(30.0)
                                .build();

                when(bookingService.getBookingsForTool(toolId)).thenReturn(List.of(booking));

                mockMvc.perform(get("/api/bookings").param("toolId", toolId.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].toolTitle", is("Hammer Drill")));
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

                when(bookingService.updateBookingStatus(bookingId, BookingStatus.APPROVED)).thenReturn(response);

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
                when(bookingService.updateBookingStatus(bookingId, BookingStatus.REJECTED))
                                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                                                "Booking decision is already final"));

                mockMvc.perform(put("/api/bookings/{bookingId}/status", bookingId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"REJECTED\"}"))
                                .andExpect(status().isBadRequest());
        }

        // ===== Condition Report Tests =====

        @Test
        @DisplayName("Should submit condition report successfully")
        void submitConditionReportSuccess() throws Exception {
                UUID bookingId = UUID.randomUUID();
                UUID reporterId = UUID.randomUUID();
                BookingResponse response = BookingResponse.builder()
                                .id(bookingId)
                                .toolId(UUID.randomUUID())
                                .renterId(reporterId)
                                .ownerId(UUID.randomUUID())
                                .startDate(LocalDate.now().minusDays(2))
                                .endDate(LocalDate.now().minusDays(1))
                                .status(BookingStatus.COMPLETED)
                                .paymentStatus(PaymentStatus.COMPLETED)
                                .totalPrice(40.0)
                                .conditionStatus(com.toolshed.backend.repository.enums.ConditionStatus.BROKEN)
                                .depositStatus(com.toolshed.backend.repository.enums.DepositStatus.REQUIRED)
                                .depositAmount(50.0)
                                .build();

                when(bookingService.submitConditionReport(any(), any())).thenReturn(response);

                String requestBody = String.format("""
                                {
                                    "conditionStatus": "BROKEN",
                                    "description": "Tool is damaged",
                                    "renterId": "%s"
                                }
                                """, reporterId);

                mockMvc.perform(post("/api/bookings/{bookingId}/condition-report", bookingId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id", is(bookingId.toString())))
                                .andExpect(jsonPath("$.conditionStatus", is("BROKEN")))
                                .andExpect(jsonPath("$.depositStatus", is("REQUIRED")))
                                .andExpect(jsonPath("$.depositAmount", is(50.0)));
        }

        @Test
        @DisplayName("Should return not found when booking does not exist for condition report")
        void submitConditionReportNotFound() throws Exception {
                UUID bookingId = UUID.randomUUID();
                UUID reporterId = UUID.randomUUID();

                when(bookingService.submitConditionReport(any(), any()))
                                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,
                                                "Booking not found"));

                String requestBody = String.format("""
                                {
                                    "conditionStatus": "OK",
                                    "description": "Good condition",
                                    "renterId": "%s"
                                }
                                """, reporterId);

                mockMvc.perform(post("/api/bookings/{bookingId}/condition-report", bookingId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return bad request when condition already reported")
        void submitConditionReportAlreadyReported() throws Exception {
                UUID bookingId = UUID.randomUUID();
                UUID reporterId = UUID.randomUUID();

                when(bookingService.submitConditionReport(any(), any()))
                                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                                                "Condition report already submitted"));

                String requestBody = String.format("""
                                {
                                    "conditionStatus": "MINOR_DAMAGE",
                                    "description": "Scratch on handle",
                                    "renterId": "%s"
                                }
                                """, reporterId);

                mockMvc.perform(post("/api/bookings/{bookingId}/condition-report", bookingId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                                .andExpect(status().isBadRequest());
        }

        // ============== Cancel Booking Tests ==============

        @Test
        @DisplayName("Should cancel booking and return refund details")
        void cancelBookingSuccess() throws Exception {
                UUID bookingId = UUID.randomUUID();
                UUID renterId = UUID.randomUUID();

                CancelBookingResponse cancelResponse = CancelBookingResponse.builder()
                                .bookingId(bookingId)
                                .status("CANCELLED")
                                .refundAmount(50.0)
                                .refundPercentage(50)
                                .message("Booking cancelled. 50% refund (€50.00) processed.")
                                .build();

                when(bookingService.cancelBooking(bookingId, renterId)).thenReturn(cancelResponse);

                mockMvc.perform(post("/api/bookings/{bookingId}/cancel", bookingId)
                                .param("renterId", renterId.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.bookingId", is(bookingId.toString())))
                                .andExpect(jsonPath("$.status", is("CANCELLED")))
                                .andExpect(jsonPath("$.refundPercentage", is(50)))
                                .andExpect(jsonPath("$.refundAmount", is(50.0)));
        }

        @Test
        @DisplayName("Should return 404 when cancelling non-existent booking")
        void cancelBookingNotFound() throws Exception {
                UUID bookingId = UUID.randomUUID();
                UUID renterId = UUID.randomUUID();

                when(bookingService.cancelBooking(bookingId, renterId))
                                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,
                                                "Booking not found"));

                mockMvc.perform(post("/api/bookings/{bookingId}/cancel", bookingId)
                                .param("renterId", renterId.toString()))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when wrong renter tries to cancel")
        void cancelBookingForbidden() throws Exception {
                UUID bookingId = UUID.randomUUID();
                UUID wrongRenterId = UUID.randomUUID();

                when(bookingService.cancelBooking(bookingId, wrongRenterId))
                                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                                                "Only the renter can cancel the booking"));

                mockMvc.perform(post("/api/bookings/{bookingId}/cancel", bookingId)
                                .param("renterId", wrongRenterId.toString()))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 400 when cancelling already cancelled booking")
        void cancelBookingBadRequest() throws Exception {
                UUID bookingId = UUID.randomUUID();
                UUID renterId = UUID.randomUUID();

                when(bookingService.cancelBooking(bookingId, renterId))
                                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                                                "Cannot cancel booking with status: CANCELLED"));

                mockMvc.perform(post("/api/bookings/{bookingId}/cancel", bookingId)
                                .param("renterId", renterId.toString()))
                                .andExpect(status().isBadRequest());
        }
}
