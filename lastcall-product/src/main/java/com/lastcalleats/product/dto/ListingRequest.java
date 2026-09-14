package com.lastcalleats.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request DTO used to create a product listing.
 *
 * A listing represents a specific product offer published by a merchant
 * for a particular day, including discount price, quantity, and pickup time.
 */
@Data
public class ListingRequest {

  /**
   * Id of the product template used to create this listing.
   */
  @NotNull
  private Long templateId;

  /**
   * Discounted selling price of the listing.
   * Must be greater than 0.
   */
  @NotNull
  @DecimalMin(value = "0.01", message = "Discount price must be greater than 0")
  private BigDecimal discountPrice;

  /**
   * Total quantity available when the listing is created.
   * Must be at least 1.
   */
  @NotNull
  @Min(value = 1, message = "Quantity must be at least 1")
  private Integer quantity;

  /**
   * Start time of the pickup window.
   */
  @NotNull
  private LocalTime pickupStart;

  /**
   * End time of the pickup window.
   */
  @NotNull
  private LocalTime pickupEnd;

  /**
   * Date on which the listing is available for pickup.
   */
  @NotNull
  private LocalDate date;
}