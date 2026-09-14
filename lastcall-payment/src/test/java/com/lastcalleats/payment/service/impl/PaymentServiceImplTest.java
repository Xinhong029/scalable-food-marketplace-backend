package com.lastcalleats.payment.service.impl;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import com.lastcalleats.order.entity.OrderDO;
import com.lastcalleats.order.entity.OrderDO.OrderStatus;
import com.lastcalleats.order.entity.PickupCodeDO;
import com.lastcalleats.order.repository.OrderRepo;
import com.lastcalleats.order.repository.PickupCodeRepo;
import com.lastcalleats.payment.dto.PaymentRequest;
import com.lastcalleats.payment.dto.PaymentResponse;
import com.lastcalleats.payment.dto.WebhookRequest;
import com.lastcalleats.payment.strategy.PaymentStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private OrderRepo orderRepo;
    @Mock private PickupCodeRepo pickupCodeRepo;
    @Mock private PaymentStrategy stripeStrategy;

    private PaymentServiceImpl paymentService;

    private OrderDO pendingOrder;
    private PaymentRequest request;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(orderRepo, pickupCodeRepo, List.of(stripeStrategy));

        pendingOrder = OrderDO.builder()
                .id(1L).userId(10L)
                .price(new BigDecimal("9.99"))
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        request = new PaymentRequest();
        request.setOrderId(1L);
        request.setPaymentMethodId("pm_test");
        request.setPaymentType("STRIPE");
    }

    private void stubStripeStrategy() {
        when(stripeStrategy.supports("STRIPE")).thenReturn(true);
    }

    @Test
    void createPaymentIntent_shouldMarkOrderPaidAndGeneratePickupCode() {
        stubStripeStrategy();
        PaymentResponse strategyResponse = PaymentResponse.builder()
                .orderId(1L).status("succeeded").paymentIntentId("pi_test").build();

        when(orderRepo.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(stripeStrategy.pay(pendingOrder, request)).thenReturn(strategyResponse);
        when(pickupCodeRepo.findByOrderId(1L)).thenReturn(Optional.empty());

        PaymentResponse response = paymentService.createPaymentIntent(10L, request);

        assertEquals(OrderStatus.PAID, pendingOrder.getStatus());
        assertEquals("succeeded", response.getStatus());
        verify(orderRepo).save(pendingOrder);
        verify(pickupCodeRepo).save(any(PickupCodeDO.class));
    }

    @Test
    void createPaymentIntent_shouldNotDuplicatePickupCodeIfAlreadyExists() {
        stubStripeStrategy();
        when(orderRepo.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(stripeStrategy.pay(pendingOrder, request)).thenReturn(
                PaymentResponse.builder().orderId(1L).status("succeeded").build());
        when(pickupCodeRepo.findByOrderId(1L)).thenReturn(Optional.of(
                PickupCodeDO.builder().orderId(1L).numericCode("111111").build()));

        paymentService.createPaymentIntent(10L, request);

        verify(pickupCodeRepo, never()).save(any());
    }

    @Test
    void createPaymentIntent_shouldThrowWhenOrderNotFound() {
        when(orderRepo.findById(1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.createPaymentIntent(10L, request));
        assertEquals(ErrorCode.ORDER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void createPaymentIntent_shouldThrowWhenUserMismatch() {
        when(orderRepo.findById(1L)).thenReturn(Optional.of(pendingOrder));

        assertThrows(BusinessException.class,
                () -> paymentService.createPaymentIntent(99L, request));
    }

    @Test
    void createPaymentIntent_shouldThrowWhenOrderAlreadyPaid() {
        pendingOrder.setStatus(OrderStatus.PAID);
        when(orderRepo.findById(1L)).thenReturn(Optional.of(pendingOrder));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.createPaymentIntent(10L, request));
        assertEquals(ErrorCode.ORDER_STATUS_INVALID, ex.getErrorCode());
    }

    @Test
    void createPaymentIntent_shouldThrowWhenPaymentTypeNotSupported() {
        request.setPaymentType("PAYPAL");
        when(orderRepo.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(stripeStrategy.supports("PAYPAL")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> paymentService.createPaymentIntent(10L, request));
        assertEquals(ErrorCode.PAYMENT_METHOD_NOT_SUPPORTED, ex.getErrorCode());
    }

    @Test
    void handleWebhook_succeeded_shouldMarkPendingOrderAsPaid() {
        WebhookRequest webhook = new WebhookRequest();
        webhook.setEventType("payment_intent.succeeded");
        webhook.setOrderId(1L);

        when(orderRepo.findById(1L)).thenReturn(Optional.of(pendingOrder));
        when(pickupCodeRepo.findByOrderId(1L)).thenReturn(Optional.empty());

        paymentService.handleWebhook(webhook);

        assertEquals(OrderStatus.PAID, pendingOrder.getStatus());
    }

    @Test
    void handleWebhook_succeeded_shouldSkipIfOrderAlreadyPaid() {
        pendingOrder.setStatus(OrderStatus.PAID);
        WebhookRequest webhook = new WebhookRequest();
        webhook.setEventType("payment_intent.succeeded");
        webhook.setOrderId(1L);

        when(orderRepo.findById(1L)).thenReturn(Optional.of(pendingOrder));

        paymentService.handleWebhook(webhook);

        verify(orderRepo, never()).save(any());
    }

    @Test
    void handleWebhook_failed_shouldCancelPendingOrder() {
        WebhookRequest webhook = new WebhookRequest();
        webhook.setEventType("payment_intent.payment_failed");
        webhook.setOrderId(1L);

        when(orderRepo.findById(1L)).thenReturn(Optional.of(pendingOrder));

        paymentService.handleWebhook(webhook);

        assertEquals(OrderStatus.CANCELLED, pendingOrder.getStatus());
        verify(orderRepo).save(pendingOrder);
    }

    @Test
    void handleWebhook_unknownEvent_shouldDoNothing() {
        WebhookRequest webhook = new WebhookRequest();
        webhook.setEventType("charge.refunded");
        webhook.setOrderId(1L);

        paymentService.handleWebhook(webhook);

        verify(orderRepo, never()).findById(any());
    }
}
