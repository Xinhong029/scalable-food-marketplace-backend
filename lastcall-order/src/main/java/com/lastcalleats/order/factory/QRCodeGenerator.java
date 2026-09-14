package com.lastcalleats.order.factory;

import com.lastcalleats.order.entity.OrderDO;
import org.springframework.stereotype.Component;

/**
 * Generates the QR payload stored with an order's pickup data. The returned string is designed for
 * frontend QR rendering instead of being a rendered image itself.
 */
@Component
public class QRCodeGenerator implements PickupCodeGenerator {

  /**
   * Returns the lookup key used by the factory for QR payloads.
   *
   * @return generator type identifier
   */
  @Override
  public String type() {
    return "QR";
  }

  /**
   * Generates a QR payload string for the supplied order.
   *
   * @param order order that needs a QR payload
   * @return generated QR payload string
   */
  @Override
  public String generate(OrderDO order) {
    return "ORDER:" + order.getId();
  }
}
