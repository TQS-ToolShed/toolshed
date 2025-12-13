package com.toolshed.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.stripe.Stripe;

import jakarta.annotation.PostConstruct;

/**
 * Stripe configuration class.
 * Initializes the Stripe API with the secret key from environment variables.
 * 
 * The secret key is read from:
 * - Environment variable: STRIPE_SECRET_KEY
 * - Or application.properties: stripe.secret-key
 * 
 * IMPORTANT: Never commit the secret key to version control!
 * 
 * Note: This configuration is disabled for tests (@Profile("!test"))
 * to avoid Spring context loading issues when STRIPE_SECRET_KEY is not set.
 */
@Configuration
@Profile("!test")  // Skip this configuration during tests
public class StripeConfig {

    private final String stripeSecretKey;

    public StripeConfig(@Value("${stripe.secret-key}") String stripeSecretKey) {
        this.stripeSecretKey = stripeSecretKey;
    }

    /**
     * Initialize Stripe API key at application startup.
     * This is called once when Spring creates this configuration bean.
     */
    @PostConstruct
    public void init() {
        // Only initialize if we have a real key (not the placeholder)
        if (stripeSecretKey != null && !stripeSecretKey.contains("your_secret_key_here")) {
            setStripeApiKey(stripeSecretKey);
        }
    }

    /**
     * Sets the Stripe API key. Extracted to a static method to satisfy SonarCloud rule
     * about modifying static fields from instance methods.
     */
    private static void setStripeApiKey(String apiKey) {
        Stripe.apiKey = apiKey;
    }
}
