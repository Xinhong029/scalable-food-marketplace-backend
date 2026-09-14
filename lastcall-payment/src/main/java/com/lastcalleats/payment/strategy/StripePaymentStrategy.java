package com.lastcalleats.payment.strategy;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import com.lastcalleats.order.entity.OrderDO;
import com.lastcalleats.payment.dto.PaymentRequest;
import com.lastcalleats.payment.dto.PaymentResponse;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * {@link PaymentStrategy} implementation backed by the Stripe Payments API.
 * All Stripe SDK calls are isolated here so the rest of the codebase stays provider-independent.
 */
@Slf4j
@Component
public class StripePaymentStrategy implements PaymentStrategy {

    /** {@inheritDoc} */
    @Override
    public boolean supports(String paymentType) {
        return "STRIPE".equalsIgnoreCase(paymentType);
    }

    /** {@inheritDoc} */
    @Override
    public PaymentResponse pay(OrderDO order, PaymentRequest request) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(order.getPrice().multiply(BigDecimal.valueOf(100)).longValue())
                    .setCurrency("usd")
                    .setPaymentMethod(request.getPaymentMethodId())
                    .setConfirm(true)
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build()
                    )
                    .putMetadata("orderId", String.valueOf(order.getId()))
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            if (!"succeeded".equals(intent.getStatus())) {
                throw new BusinessException(ErrorCode.PAYMENT_FAILED);
            }

            return PaymentResponse.builder()
                    .orderId(order.getId())
                    .status("succeeded")
                    .paymentIntentId(intent.getId())
                    .build();

        } catch (StripeException e) {
            log.error("Stripe payment failed, orderId={}", order.getId());
            throw new BusinessException(ErrorCode.PAYMENT_FAILED, e.getMessage());
        }
    }
}
