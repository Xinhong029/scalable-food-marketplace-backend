package com.lastcalleats.order.state;

import com.lastcalleats.order.entity.OrderDO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the state transitions allowed by {@link PendingPaymentState}.
 * These cases confirm that unpaid orders can either be paid or cancelled from the initial state.
 */
class PendingPaymentStateTest {

  private final PendingPaymentState state = new PendingPaymentState();

  /**
   * Verifies that a pending-payment order can transition to the paid state.
   */
  @Test
  void pay_shouldMoveOrderToPaid() {
    OrderDO order = OrderDO.builder().status(OrderDO.OrderStatus.PENDING_PAYMENT).build();

    state.pay(order);

    assertEquals(OrderDO.OrderStatus.PAID, order.getStatus());
  }

  /**
   * Verifies that a pending-payment order can transition to the cancelled state.
   */
  @Test
  void cancel_shouldMoveOrderToCancelled() {
    OrderDO order = OrderDO.builder().status(OrderDO.OrderStatus.PENDING_PAYMENT).build();

    state.cancel(order);

    assertEquals(OrderDO.OrderStatus.CANCELLED, order.getStatus());
  }
}
