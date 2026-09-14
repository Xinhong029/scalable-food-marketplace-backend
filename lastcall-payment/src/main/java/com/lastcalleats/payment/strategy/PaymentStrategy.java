package com.lastcalleats.payment.strategy;

import com.lastcalleats.order.entity.OrderDO;
import com.lastcalleats.payment.dto.PaymentRequest;
import com.lastcalleats.payment.dto.PaymentResponse;

/**
 * Strategy interface for payment provider integrations.
 * Adding a new provider requires only a new implementation; no service layer changes needed.
 */
public interface PaymentStrategy {

    /**
     * @param paymentType case-insensitive token, e.g. {@code "STRIPE"}
     * @return {@code true} if this strategy handles that payment type
     */
    boolean supports(String paymentType);

    /**
     * @param order   the order being paid; its price is used as the charge amount
     * @param request contains the provider-specific payment method token
     * @return intent ID and status from the provider
     * @throws com.lastcalleats.common.exception.BusinessException on provider rejection or SDK error
     */
    PaymentResponse pay(OrderDO order, PaymentRequest request);
}

