package com.lastcalleats.order.dto;

import lombok.Data;

/**
 * Carries the merchant input used to verify an order at pickup time. Callers can provide either the
 * manual numeric code or the QR payload produced for the order.
 */
@Data
public class CodeRequest {

  /**
   * Optional six-digit code for manual verification.
   */
  private String pickupCode;

  /**
   * Optional QR string for scan verification.
   */
  private String qrCodeContent;
}
