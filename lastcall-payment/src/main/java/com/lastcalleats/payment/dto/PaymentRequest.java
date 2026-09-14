package com.lastcalleats.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for initiating a payment. The client obtains the payment method token
 * from the provider's SDK and sends it here for server-side charge confirmation.
 */
@Data
public class PaymentRequest {

    /** ID of the order the user is paying for. */
    @NotNull
    private Long orderId;

    // Payment method ID passed from Stripe → frontend → backend
    @NotNull
    private String paymentMethodId;

    // Routes to the matching PaymentStrategy; e.g. "STRIPE", "APPLE_PAY", "PAYPAL"
    @NotNull
    private String paymentType;
}
