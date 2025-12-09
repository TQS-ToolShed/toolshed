package com.toolshed.backend.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Stripe configuration class.
 * Initializes the Stripe API with the secret key from environment variables.
 * 
 * The secret key is read from:
 * - Environment variable: STRIPE_SECRET_KEY
 * - Or application.properties: stripe.secret-key
 * 
 * IMPORTANT: Never commit the secret key to version control!
 */
@Configuration
public class StripeConfig {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    /**
     * Initialize Stripe API key at application startup.
     * This is called once when Spring creates this configuration bean.
     */
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }
}
