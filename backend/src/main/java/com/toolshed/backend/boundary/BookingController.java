package com.toolshed.backend.boundary;

import com.toolshed.backend.dto.BookingResponse;
import com.toolshed.backend.dto.ConditionReportRequest;
import com.toolshed.backend.dto.CreateBookingRequest;
import com.toolshed.backend.dto.OwnerBookingResponse;
import com.toolshed.backend.dto.OwnerEarningsResponse;
import com.toolshed.backend.dto.UpdateBookingStatusRequest;
import com.toolshed.backend.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Bookings", description = "Operations related to tool bookings")
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Operation(summary = "Create a booking", description = "Validates dates (no past dates, end after start) and prevents overlaps.")
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.ok(response);
    }

    // Query-parameter variant to align with other endpoints that avoid extra path
    // segments
    @Operation(summary = "Get bookings for owner", description = "Lists booking requests for a specific owner")
    @GetMapping(params = "ownerId")
    public ResponseEntity<List<OwnerBookingResponse>> getBookingsForOwnerQuery(@RequestParam UUID ownerId) {
        List<OwnerBookingResponse> responses = bookingService.getBookingsForOwner(ownerId);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Get bookings for renter", description = "Lists booking records for a specific renter")
    @GetMapping(params = "renterId")
    public ResponseEntity<List<BookingResponse>> getBookingsForRenter(@RequestParam UUID renterId) {
        List<BookingResponse> responses = bookingService.getBookingsForRenter(renterId);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Get bookings for tool", description = "Lists bookings for a specific tool")
    @GetMapping(params = "toolId")
    public ResponseEntity<List<BookingResponse>> getBookingsForTool(@RequestParam UUID toolId) {
        List<BookingResponse> responses = bookingService.getBookingsForTool(toolId);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Update booking status", description = "Approve or reject a booking request")
    @PutMapping("/{bookingId}/status")
    public ResponseEntity<BookingResponse> updateBookingStatus(
            @PathVariable UUID bookingId,
            @Valid @RequestBody UpdateBookingStatusRequest request) {
        BookingResponse response = bookingService.updateBookingStatus(bookingId, request.getStatus());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Submit condition report", description = "Renter reports the condition of the tool at the end of rental. Damage triggers deposit requirement.")
    @PostMapping("/{bookingId}/condition-report")
    public ResponseEntity<BookingResponse> submitConditionReport(
            @PathVariable UUID bookingId,
            @Valid @RequestBody ConditionReportRequest request) {
        BookingResponse response = bookingService.submitConditionReport(bookingId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Pay deposit", description = "Renter pays the security deposit required due to damage report.")
    @PostMapping("/{bookingId}/pay-deposit")
    public ResponseEntity<BookingResponse> payDeposit(
            @PathVariable UUID bookingId,
            @RequestParam UUID renterId) {
        BookingResponse response = bookingService.payDeposit(bookingId, renterId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get owner earnings", description = "Get earnings breakdown by month for a specific owner")
    @GetMapping("/earnings")
    public ResponseEntity<OwnerEarningsResponse> getOwnerEarnings(@RequestParam UUID ownerId) {
        OwnerEarningsResponse response = bookingService.getOwnerEarnings(ownerId);
        return ResponseEntity.ok(response);
    }
}
