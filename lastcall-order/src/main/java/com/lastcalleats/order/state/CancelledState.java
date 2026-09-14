package com.lastcalleats.order.state;

import com.lastcalleats.order.entity.OrderDO;

/**
 * Represents orders that were cancelled before fulfillment completed. It acts as a terminal state
 * so invalid follow-up transitions fail through the default state interface behavior.
 */
public class CancelledState implements OrderState {

  /**
   * Returns the order status represented by this state object.
   *
   * @return cancelled status
   */
  @Override
  public OrderDO.OrderStatus getStatus() {
    return OrderDO.OrderStatus.CANCELLED;
  }
}
