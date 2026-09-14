package com.lastcalleats.order.factory;

import com.lastcalleats.order.entity.OrderDO;

/**
 * Defines a strategy for generating pickup credentials for an order. Implementations allow the
 * module to support multiple code formats without changing the calling service.
 */
public interface PickupCodeGenerator {

  /**
   * Gets the type key for this generator.
   *
   * @return generator type
   */
  String type();

  /**
   * Generates a pickup code for the given order.
   *
   * @param order target order
   * @return generated pickup code
   */
  String generate(OrderDO order);
}
