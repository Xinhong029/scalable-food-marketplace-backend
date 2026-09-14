package com.lastcalleats.order.state;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.order.entity.OrderDO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the terminal order states that should reject any further lifecycle changes.
 * The assertions ensure that completed and cancelled orders keep their status and block invalid transitions.
 */
class TerminalStateTest {

  /**
   * Verifies that the completed state rejects any additional transitions.
   */
  @Test
  void completedState_shouldRejectFurtherTransitions() {
    CompletedState state = new CompletedState();
    OrderDO order = OrderDO.builder().status(OrderDO.OrderStatus.COMPLETED).build();

    assertEquals(OrderDO.OrderStatus.COMPLETED, state.getStatus());
    assertThrows(BusinessException.class, () -> state.pay(order));
    assertThrows(BusinessException.class, () -> state.cancel(order));
  }

  /**
   * Verifies that the cancelled state rejects any additional transitions.
   */
  @Test
  void cancelledState_shouldRejectFurtherTransitions() {
    CancelledState state = new CancelledState();
    OrderDO order = OrderDO.builder().status(OrderDO.OrderStatus.CANCELLED).build();

    assertEquals(OrderDO.OrderStatus.CANCELLED, state.getStatus());
    assertThrows(BusinessException.class, () -> state.pay(order));
    assertThrows(BusinessException.class, () -> state.complete(order));
  }
}
