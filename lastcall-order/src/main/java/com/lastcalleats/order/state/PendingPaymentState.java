package com.lastcalleats.order.state;

import com.lastcalleats.order.entity.OrderDO;

/**
 * Represents orders that have been created but not yet paid. This state allows payment or
 * cancellation while rejecting transitions that would skip required steps in the lifecycle.
 */
public class PendingPaymentState implements OrderState {

  /**
   * Returns the order status represented by this state object.
   *
   * @return pending payment status
   */
  @Override
  public OrderDO.OrderStatus getStatus() {
    return OrderDO.OrderStatus.PENDING_PAYMENT;
  }

  /**
   * Transitions an order from pending payment to paid.
   *
   * @param order order to update
   */
  @Override
  public void pay(OrderDO order) {
    order.setStatus(OrderDO.OrderStatus.PAID);
  }

  /**
   * Transitions an order from pending payment to cancelled.
   *
   * @param order order to update
   */
  @Override
  public void cancel(OrderDO order) {
    order.setStatus(OrderDO.OrderStatus.CANCELLED);
  }
}
