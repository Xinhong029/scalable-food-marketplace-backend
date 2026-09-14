package com.lastcalleats.order.factory;

import com.lastcalleats.common.util.PickupCodeUtil;
import com.lastcalleats.order.entity.OrderDO;
import org.springframework.stereotype.Component;

/**
 * Generates short numeric pickup codes for manual verification flows. This generator is used when
 * the customer or merchant needs a compact code that can be entered without scanning.
 */
@Component
public class NumericCodeGenerator implements PickupCodeGenerator {

  /**
   * Returns the lookup key used by the factory for numeric codes.
   *
   * @return generator type identifier
   */
  @Override
  public String type() {
    return "NUMERIC";
  }

  /**
   * Generates a numeric pickup code for the supplied order.
   *
   * @param order order that needs a pickup code
   * @return generated numeric pickup code
   */
  @Override
  public String generate(OrderDO order) {
    return PickupCodeUtil.generateNumericCode();
  }
}
