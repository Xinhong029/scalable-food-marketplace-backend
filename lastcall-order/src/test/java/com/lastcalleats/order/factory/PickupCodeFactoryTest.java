package com.lastcalleats.order.factory;

import com.lastcalleats.order.entity.OrderDO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the generator selection behavior provided by {@link PickupCodeFactory}.
 * These cases verify that the factory delegates to the correct generator and rejects unsupported generator types.
 */
class PickupCodeFactoryTest {

  private final PickupCodeFactory factory = new PickupCodeFactory(List.of(
      new NumericCodeGenerator(),
      new QRCodeGenerator()
  ));

  /**
   * Verifies that the factory returns a six-digit numeric code for the numeric generator type.
   */
  @Test
  void generate_shouldReturnNumericCodeForNumericType() {
    String result = factory.generate(buildOrder(), "NUMERIC");

    assertEquals(6, result.length());
    assertTrue(result.chars().allMatch(Character::isDigit));
  }

  /**
   * Verifies that the factory returns the QR payload generated for the QR generator type.
   */
  @Test
  void generate_shouldReturnQrPayloadForQrType() {
    String result = factory.generate(buildOrder(), "QR");

    assertEquals("ORDER:10", result);
  }

  /**
   * Verifies that the factory rejects requests for an unsupported generator type.
   */
  @Test
  void generate_shouldRejectUnknownType() {
    assertThrows(RuntimeException.class, () -> factory.generate(buildOrder(), "UNKNOWN"));
  }

  /**
   * Creates a paid order used by the pickup code generation tests.
   *
   * @return order fixture for the factory test cases
   */
  private OrderDO buildOrder() {
    return OrderDO.builder()
        .id(10L)
        .userId(20L)
        .merchantId(30L)
        .listingId(40L)
        .status(OrderDO.OrderStatus.PAID)
        .build();
  }
}
