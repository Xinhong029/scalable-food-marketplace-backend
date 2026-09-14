package com.lastcalleats.payment.service.impl;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import com.lastcalleats.common.util.Assert;
import com.lastcalleats.common.util.PickupCodeUtil;
import com.lastcalleats.order.entity.OrderDO;
import com.lastcalleats.order.entity.OrderDO.OrderStatus;
import com.lastcalleats.order.entity.PickupCodeDO;
import com.lastcalleats.order.repository.OrderRepo;
import com.lastcalleats.order.repository.PickupCodeRepo;
import com.lastcalleats.payment.dto.PaymentRequest;
import com.lastcalleats.payment.dto.PaymentResponse;
import com.lastcalleats.payment.dto.WebhookRequest;
import com.lastcalleats.payment.service.PaymentService;
import com.lastcalleats.payment.strategy.PaymentStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Default implementation of {@link PaymentService}. Selects the right {@link com.lastcalleats.payment.strategy.PaymentStrategy}
 * at runtime and issues a pickup code once an order transitions to PAID.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
  private final OrderRepo orderRepo;
  private final PickupCodeRepo pickupCodeRepo;
  private final List<PaymentStrategy> strategies;

  /** {@inheritDoc} Guards against duplicate pickup codes before persisting a new one. */
  @Override
  @Transactional
  public PaymentResponse createPaymentIntent(Long userId, PaymentRequest request) {
    OrderDO order = orderRepo.findById(request.getOrderId())
        .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    Assert.equals(order.getUserId(), userId, ErrorCode.BAD_REQUEST);
    Assert.isTrue(order.getStatus() == OrderStatus.PENDING_PAYMENT, ErrorCode.ORDER_STATUS_INVALID);

    PaymentStrategy strategy = strategies.stream()
        .filter(s -> s.supports(request.getPaymentType()))
        .findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_METHOD_NOT_SUPPORTED));

    PaymentResponse response = strategy.pay(order, request);
    markOrderPaid(order);
    return response;
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  public void handleWebhook(WebhookRequest request) {
    switch (request.getEventType()) {
      case "payment_intent.succeeded" -> {
        OrderDO order = orderRepo.findById(request.getOrderId())
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
          markOrderPaid(order);
        }
      }
      case "payment_intent.payment_failed" -> {
        OrderDO order = orderRepo.findById(request.getOrderId()).orElse(null);
        if (order != null && order.getStatus() == OrderStatus.PENDING_PAYMENT) {
          order.setStatus(OrderStatus.CANCELLED);
          orderRepo.save(order);
        }
      }
      default -> log.debug("Unhandled webhook event: {}", request.getEventType());
    }
  }

  /**
   * Sets the order to PAID and generates a pickup code if one does not already exist.
   *
   * @param order the order to transition
   */
  private void markOrderPaid(OrderDO order) {
    order.setStatus(OrderStatus.PAID);
    orderRepo.save(order);
    if (pickupCodeRepo.findByOrderId(order.getId()).isEmpty()) {
      pickupCodeRepo.save(PickupCodeDO.builder()
          .orderId(order.getId())
          .numericCode(PickupCodeUtil.generateNumericCode())
          .qrCode("ORDER:" + order.getId())
          .used(false)
          .build());
    }
  }
}
