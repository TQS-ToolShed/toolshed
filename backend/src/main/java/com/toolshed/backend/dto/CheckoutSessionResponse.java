package com.toolshed.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO containing the Stripe Checkout Session information.
 * The frontend uses the checkoutUrl to redirect the user to Stripe's hosted payment page.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutSessionResponse {

    /**
     * The Stripe session ID (e.g., "cs_test_123...").
     * Can be used with stripe.redirectToCheckout({ sessionId }) on the frontend.
     */
    private String sessionId;

    /**
     * The full URL to the Stripe Checkout page.
     * The frontend can simply redirect to this URL: window.location.href = checkoutUrl
     */
    private String checkoutUrl;
}
