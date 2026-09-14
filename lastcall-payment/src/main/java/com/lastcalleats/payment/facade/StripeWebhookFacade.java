package com.lastcalleats.payment.facade;

import com.lastcalleats.payment.config.StripeConfig;
import com.lastcalleats.payment.dto.WebhookRequest;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Facade that handles Stripe signature verification, event deserialization, and metadata extraction.
 * The controller receives a plain {@link WebhookRequest} and stays decoupled from the Stripe SDK.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StripeWebhookFacade {

    private final StripeConfig stripeConfig;

    /** Parses and verifies a Stripe webhook. Returns empty on invalid signature or unrecognized payload. */
    public Optional<WebhookRequest> parse(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature");
            return Optional.empty();
        }

        Optional<StripeObject> stripeObject = event.getDataObjectDeserializer().getObject();
        if (stripeObject.isEmpty()) {
            log.warn("Failed to deserialize Stripe event: {}", event.getType());
            return Optional.empty();
        }

        if (!(stripeObject.get() instanceof PaymentIntent intent)) {
            return Optional.empty();
        }

        String orderIdStr = intent.getMetadata().get("orderId");
        if (orderIdStr == null) {
            log.warn("PaymentIntent missing orderId metadata: {}", intent.getId());
            return Optional.empty();
        }

        WebhookRequest request = new WebhookRequest();
        request.setEventId(event.getId());
        request.setEventType(event.getType());
        request.setOrderId(Long.parseLong(orderIdStr));
        return Optional.of(request);
    }
}
