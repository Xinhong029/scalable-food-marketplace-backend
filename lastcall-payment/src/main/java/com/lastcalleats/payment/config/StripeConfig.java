package com.lastcalleats.payment.config;

import com.stripe.Stripe;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Binds Stripe credentials from {@code app.stripe.*} and initializes the SDK's global API key on startup.
 * The webhook secret is used by {@link com.lastcalleats.payment.facade.StripeWebhookFacade} to verify signatures.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.stripe")
public class StripeConfig {

    private String secretKey;
    private String webhookSecret;

    /** Sets {@link Stripe#apiKey} so all subsequent SDK calls are authenticated. */
    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }
}
