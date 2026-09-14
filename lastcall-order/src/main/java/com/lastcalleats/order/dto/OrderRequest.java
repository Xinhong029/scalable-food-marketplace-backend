package com.lastcalleats.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Carries the client input required to create an order. The payload stays intentionally small
 * because the listing determines the merchant, pricing, and other order details.
 */
@Data
public class OrderRequest {

  // @Data creates getter, setter, toString, hashCode, and equals methods.

  @NotNull
  private Long listingId;
}
