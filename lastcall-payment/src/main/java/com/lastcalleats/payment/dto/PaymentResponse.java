package com.lastcalleats.payment.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Response returned after a payment intent is created or confirmed.
 * When {@code requiresAction} is true, the client must complete an extra step (e.g. 3D Secure).
 */
@Getter
@Builder
public class PaymentResponse {

    private Long orderId;
    private String status;
    private String paymentIntentId;

    // True when the client must handle an extra step (e.g. 3D Secure)
    private Boolean requiresAction;
    private String clientSecret;
}
