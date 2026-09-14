package com.lastcalleats.order.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Stable message contract published after an order is created. */
public record OrderCreatedEvent(
    UUID eventId,
    String eventType,
    Long orderId,
    Long userId,
    Long merchantId,
    Long listingId,
    BigDecimal price,
    Instant occurredAt
) {

  public static final String TYPE = "ORDER_CREATED";

  /** Creates a new event with a unique id and UTC occurrence time. */
  public static OrderCreatedEvent create(
      Long orderId,
      Long userId,
      Long merchantId,
      Long listingId,
      BigDecimal price) {
    return new OrderCreatedEvent(
        UUID.randomUUID(), TYPE, orderId, userId, merchantId, listingId, price, Instant.now());
  }
}
