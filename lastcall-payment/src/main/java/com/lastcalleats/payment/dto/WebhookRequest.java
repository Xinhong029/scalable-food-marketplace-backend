package com.lastcalleats.payment.dto;

import lombok.Data;

/**
 * Provider-agnostic representation of a webhook event. Translated from the raw Stripe payload
 * by {@link com.lastcalleats.payment.facade.StripeWebhookFacade} to decouple the service layer from the SDK.
 */
@Data
public class WebhookRequest {

    private String eventId;   // for idempotency
    private String eventType; // e.g. "payment_intent.succeeded"
    private Long orderId;     // extracted from PaymentIntent metadata
}
