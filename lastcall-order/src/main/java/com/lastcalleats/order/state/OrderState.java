package com.lastcalleats.order.state;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import com.lastcalleats.order.entity.OrderDO;

/**
 * Defines the operations allowed for a specific order lifecycle state. Implementations override
 * only the transitions that are valid for their status and rely on default failures for the rest.
 */
public interface OrderState {

  /**
   * Gets the status for this state object.
   *
   * @return order status
   */
  OrderDO.OrderStatus getStatus();

  /**
   * Changes an order to PAID when allowed.
   *
   * @param order target order
   */
  default void pay(OrderDO order) {
    throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
  }

  /**
   * Changes an order to COMPLETED when allowed.
   *
   * @param order target order
   */
  default void complete(OrderDO order) {
    throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
  }

  /**
   * Changes an order to CANCELLED when allowed.
   *
   * @param order target order
   */
  default void cancel(OrderDO order) {
    throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID);
  }
}
