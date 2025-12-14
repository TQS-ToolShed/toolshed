package com.toolshed.backend.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toolshed.backend.dto.CheckoutSessionResponse;
import com.toolshed.backend.dto.SubscriptionStatusResponse;
import com.toolshed.backend.service.SubscriptionException;
import com.toolshed.backend.service.SubscriptionService;
import com.toolshed.backend.service.UserNotFoundException;

/**
 * REST Controller for subscription management.
 */
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @Value("${stripe.subscription-success-url:http://localhost:5173/subscription/success}")
    private String successUrl;

    @Value("${stripe.subscription-cancel-url:http://localhost:5173/subscription/cancel}")
    private String cancelUrl;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /**
     * Creates a Stripe Checkout Session for Pro Member subscription.
     */
    @PostMapping("/pro/{userId}")
    public ResponseEntity<CheckoutSessionResponse> createProSubscription(@PathVariable UUID userId) {
        try {
            CheckoutSessionResponse response = subscriptionService.createProSubscription(userId, successUrl, cancelUrl);
            return ResponseEntity.ok(response);
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (SubscriptionException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Handles Stripe webhook events for subscription updates.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        subscriptionService.handleSubscriptionWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }

    /**
     * Activates Pro subscription after successful checkout.
     * Called from the frontend after Stripe checkout completes.
     */
    @PostMapping("/activate/{userId}")
    public ResponseEntity<SubscriptionStatusResponse> activateSubscription(
            @PathVariable UUID userId,
            @RequestBody ActivateSubscriptionRequest request) {
        try {
            subscriptionService.activateProSubscription(userId, request.getStripeSubscriptionId());
            SubscriptionStatusResponse status = subscriptionService.getSubscriptionStatus(userId);
            return ResponseEntity.ok(status);
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Cancels a user's Pro subscription.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> cancelSubscription(@PathVariable UUID userId) {
        try {
            subscriptionService.cancelSubscription(userId);
            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (SubscriptionException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Gets the subscription status for a user.
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<SubscriptionStatusResponse> getSubscriptionStatus(@PathVariable UUID userId) {
        try {
            SubscriptionStatusResponse status = subscriptionService.getSubscriptionStatus(userId);
            return ResponseEntity.ok(status);
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Request DTO for activating subscription.
     */
    public static class ActivateSubscriptionRequest {
        private String stripeSubscriptionId;

        public String getStripeSubscriptionId() {
            return stripeSubscriptionId;
        }

        public void setStripeSubscriptionId(String stripeSubscriptionId) {
            this.stripeSubscriptionId = stripeSubscriptionId;
        }
    }
}
