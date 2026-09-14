package com.lastcalleats.order.state;

import com.lastcalleats.order.entity.OrderDO;

/**
 * Represents orders that have been paid and are ready for fulfillment. This state allows completion
 * when pickup succeeds while preventing unsupported backward transitions.
 */
public class PaidState implements OrderState {

  /**
   * Returns the order status represented by this state object.
   *
   * @return paid status
   */
  @Override
  public OrderDO.OrderStatus getStatus() {
    return OrderDO.OrderStatus.PAID;
  }

  /**
   * Transitions an order from paid to completed.
   *
   * @param order order to update
   */
  @Override
  public void complete(OrderDO order) {
    order.setStatus(OrderDO.OrderStatus.COMPLETED);
  }
}
