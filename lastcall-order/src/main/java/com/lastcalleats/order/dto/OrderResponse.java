package com.lastcalleats.order.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents the order data returned by customer and merchant order endpoints. It combines
 * persisted order fields with product display data and optional pickup credentials for paid
 * orders.
 */
@Getter
@Builder
public class OrderResponse {

  private Long id;
  private Long listingId;
  private Long merchantId;
  private String productName;
  private BigDecimal price;
  private String status;

  // Only returned after payment succeeds.
  private String pickupCode;

  // Added in V1: the raw QR string for the frontend.
  // The frontend should use a QR library to turn this string into a real QR image.
  private String qrCodeContent;

  private LocalDateTime createdAt;
}
