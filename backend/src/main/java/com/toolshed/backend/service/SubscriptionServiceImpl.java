package com.toolshed.backend.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.toolshed.backend.dto.CheckoutSessionResponse;
import com.toolshed.backend.dto.SubscriptionStatusResponse;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.SubscriptionTier;

/**
 * Implementation of SubscriptionService for Pro Member subscriptions.
 * Uses one-time payment (like tool bookings) instead of recurring billing.
 */
@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final double PRO_DISCOUNT_PERCENTAGE = 5.0;
    private static final double PRO_PRICE_EUR = 25.0;
    private static final long PRO_PRICE_CENTS = 2500L;
    private static final String USER_NOT_FOUND_MSG = "User not found: ";

    private final UserRepository userRepository;

    @Value("${stripe.enabled:true}")
    private boolean stripeEnabled;

    public SubscriptionServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public CheckoutSessionResponse createProSubscription(UUID userId, String successUrl, String cancelUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MSG + userId));

        // Check if already a Pro member
        if (isProMember(user)) {
            throw new SubscriptionException("User already has an active Pro subscription");
        }

        // DEV MODE: Activate directly without Stripe payment
        if (!stripeEnabled) {
            activateProSubscription(userId, "dev_" + UUID.randomUUID().toString());

            return CheckoutSessionResponse.builder()
                    .sessionId("dev_session")
                    .checkoutUrl(successUrl + "?session_id=dev_activated")
                    .build();
        }

        // PRODUCTION: Create one-time Stripe checkout (like tool booking payment)
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT) // One-time payment, not subscription
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("eur")
                                                    .setUnitAmount(PRO_PRICE_CENTS)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Pro Member - Lifetime Access")
                                                                    .setDescription(
                                                                            "Get 5% discount on all tool rentals")
                                                                    .build())
                                                    .build())
                                    .setQuantity(1L)
                                    .build())
                    .putMetadata("userId", userId.toString())
                    .putMetadata("paymentType", "pro_subscription")
                    .build();

            Session session = Session.create(params);

            return CheckoutSessionResponse.builder()
                    .sessionId(session.getId())
                    .checkoutUrl(session.getUrl())
                    .build();
        } catch (StripeException e) {
            throw new SubscriptionException("Failed to create payment session: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void handleSubscriptionWebhook(String payload, String signature) {
        // Webhook handling for payment confirmation
        // In production, verify signature and activate subscription on successful
        // payment
    }

    /**
     * Activates Pro subscription for a user after successful payment.
     * User stays Pro forever until they cancel.
     */
    @Override
    @Transactional
    public void activateProSubscription(UUID userId, String paymentSessionId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MSG + userId));

        user.setSubscriptionTier(SubscriptionTier.PRO);
        user.setSubscriptionStart(LocalDateTime.now());
        user.setSubscriptionEnd(null); // No expiry - lifetime until cancelled
        user.setStripeSubscriptionId(paymentSessionId);

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void cancelSubscription(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MSG + userId));

        if (user.getSubscriptionTier() != SubscriptionTier.PRO) {
            throw new SubscriptionException("User does not have an active Pro subscription");
        }

        // Simply reset to FREE tier - no Stripe cancellation needed for one-time
        // payment
        user.setSubscriptionTier(SubscriptionTier.FREE);
        user.setSubscriptionEnd(LocalDateTime.now());
        user.setStripeSubscriptionId(null);

        userRepository.save(user);
    }

    @Override
    public SubscriptionStatusResponse getSubscriptionStatus(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_MSG + userId));

        boolean isActive = isProMember(user);

        return SubscriptionStatusResponse.builder()
                .tier(user.getSubscriptionTier() != null ? user.getSubscriptionTier() : SubscriptionTier.FREE)
                .active(isActive)
                .subscriptionStart(user.getSubscriptionStart())
                .subscriptionEnd(user.getSubscriptionEnd())
                .discountPercentage(isActive ? PRO_DISCOUNT_PERCENTAGE : 0.0)
                .build();
    }

    @Override
    public boolean isProMember(User user) {
        if (user.getSubscriptionTier() != SubscriptionTier.PRO) {
            return false;
        }

        // If subscriptionEnd is null, user is Pro forever (lifetime)
        LocalDateTime end = user.getSubscriptionEnd();
        if (end == null) {
            return true;
        }

        // Check if subscription has been cancelled (end date passed)
        return LocalDateTime.now().isBefore(end);
    }

    @Override
    public double getDiscountPercentage(User user) {
        return isProMember(user) ? PRO_DISCOUNT_PERCENTAGE : 0.0;
    }
}
