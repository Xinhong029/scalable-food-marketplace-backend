package com.lastcalleats.payment.service;

import com.lastcalleats.payment.dto.PaymentRequest;
import com.lastcalleats.payment.dto.PaymentResponse;
import com.lastcalleats.payment.dto.WebhookRequest;

/**
 * Contract for payment processing. Covers intent creation and handling
 * asynchronous payment events forwarded from the provider's webhook.
 */
public interface PaymentService {

  /**
   * Creates and immediately confirms a payment intent for the given order.
   *
   * @param userId  the authenticated user initiating the payment
   * @param request order ID, payment method token, and payment type
   * @return intent ID and final status
   * @throws com.lastcalleats.common.exception.BusinessException if the order is not found,
   *         belongs to a different user, or is not in PENDING_PAYMENT state
   */
  PaymentResponse createPaymentIntent(Long userId, PaymentRequest request);

  /**
   * Processes an inbound webhook event; unknown event types are silently ignored.
   *
   * @param request parsed, provider-agnostic representation of the event
   */
  void handleWebhook(WebhookRequest request);
}
