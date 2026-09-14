package com.lastcalleats.order.state;

import com.lastcalleats.order.entity.OrderDO;

/**
 * Represents orders whose pickup flow has been finished successfully. It serves as a terminal
 * lifecycle state so later transitions can be rejected by the default behavior in
 * {@link OrderState}.
 */
public class CompletedState implements OrderState {

  /**
   * Returns the order status represented by this state object.
   *
   * @return completed status
   */
  @Override
  public OrderDO.OrderStatus getStatus() {
    return OrderDO.OrderStatus.COMPLETED;
  }
}
