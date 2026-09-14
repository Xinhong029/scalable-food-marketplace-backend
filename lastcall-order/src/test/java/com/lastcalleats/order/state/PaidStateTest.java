package com.lastcalleats.order.state;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.order.entity.OrderDO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the state transitions allowed by {@link PaidState}.
 * The assertions verify that paid orders can complete and still reject unsupported transitions.
 */
class PaidStateTest {

  private final PaidState state = new PaidState();

  /**
   * Verifies that a paid order can transition to the completed state.
   */
  @Test
  void complete_shouldMoveOrderToCompleted() {
    OrderDO order = OrderDO.builder().status(OrderDO.OrderStatus.PAID).build();

    state.complete(order);

    assertEquals(OrderDO.OrderStatus.COMPLETED, order.getStatus());
  }

  /**
   * Verifies that a paid order cannot transition directly to the cancelled state.
   */
  @Test
  void cancel_shouldThrowBecauseTransitionIsNotAllowed() {
    OrderDO order = OrderDO.builder().status(OrderDO.OrderStatus.PAID).build();

    assertThrows(BusinessException.class, () -> state.cancel(order));
  }
}
