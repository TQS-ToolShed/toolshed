package com.toolshed.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for creating a Stripe Checkout Session.
 * 
 * Note: In a production environment, you should NOT trust amountInCents from the client.
 * Instead, fetch the booking from the database using bookingId and calculate the amount server-side.
 * For this demo/test setup, we accept it from the client for simplicity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCheckoutSessionRequest {

    /**
     * The booking ID that this payment is for.
     * Used to mark the booking as PAID after successful payment.
     */
    @NotNull(message = "Booking ID is required")
    private UUID bookingId;

    /**
     * Amount to charge in cents (e.g., 2500 = 25.00 EUR).
     * In production, this should be fetched from DB, not trusted from client.
     */
    @NotNull(message = "Amount is required")
    @Min(value = 50, message = "Minimum amount is 50 cents")
    private Long amountInCents;

    /**
     * Description shown on the Stripe checkout page.
     * E.g., "Reserva Martelo #123"
     */
    @NotBlank(message = "Description is required")
    private String description;
}
