package com.lastcalleats.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request DTO used to create or update a product template.
 *
 * A product template represents the base information of a product,
 * including its name, description, and original price. Templates
 * are later used by merchants to create product listings.
 */
@Data
public class TemplateRequest {

  /**
   * Name of the product template.
   * Must not be blank.
   */
  @NotBlank
  private String name;

  /**
   * Description of the product template.
   * This field is optional.
   */
  private String description;

  /**
   * Original price of the product before any discount.
   * Must be greater than 0.
   */
  @NotNull
  @DecimalMin(value = "0.01", message = "Original price must be greater than 0")
  private BigDecimal originalPrice;
}