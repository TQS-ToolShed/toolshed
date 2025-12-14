package com.toolshed.backend.service;

import java.util.UUID;

import com.toolshed.backend.dto.CheckoutSessionResponse;
import com.toolshed.backend.dto.SubscriptionStatusResponse;
import com.toolshed.backend.repository.entities.User;

/**
 * Service interface for managing Pro Member subscriptions.
 */
public interface SubscriptionService {

    /**
     * Creates a Stripe Checkout Session for Pro Member subscription.
     * 
     * @param userId     The user who wants to subscribe
     * @param successUrl URL to redirect after successful payment
     * @param cancelUrl  URL to redirect if payment is cancelled
     * @return CheckoutSessionResponse with Stripe session details
     */
    CheckoutSessionResponse createProSubscription(UUID userId, String successUrl, String cancelUrl);

    /**
     * Handles Stripe webhook events for subscription updates.
     * 
     * @param payload   The raw webhook payload
     * @param signature The Stripe signature header
     */
    void handleSubscriptionWebhook(String payload, String signature);

    /**
     * Cancels a user's Pro Member subscription.
     * 
     * @param userId The user whose subscription to cancel
     */
    void cancelSubscription(UUID userId);

    /**
     * Gets the subscription status for a user.
     * 
     * @param userId The user to check
     * @return SubscriptionStatusResponse with current subscription details
     */
    SubscriptionStatusResponse getSubscriptionStatus(UUID userId);

    /**
     * Checks if a user is currently an active Pro Member.
     * 
     * @param user The user to check
     * @return true if user has active Pro subscription
     */
    boolean isProMember(User user);

    /**
     * Gets the discount percentage for a user based on their subscription.
     * Pro Members get 5% discount, Free members get 0%.
     * 
     * @param user The user to check
     * @return Discount percentage (0.0 to 100.0)
     */
    double getDiscountPercentage(User user);

    /**
     * Activates a Pro subscription for a user after successful Stripe checkout.
     * 
     * @param userId               The user to activate subscription for
     * @param stripeSubscriptionId The Stripe subscription ID
     */
    void activateProSubscription(UUID userId, String stripeSubscriptionId);
}
