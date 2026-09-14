package com.lastcalleats.product.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * Response DTO representing a product template.
 *
 * A product template stores reusable product information such as name,
 * description, and original price. It is used by merchants to create listings.
 */
@Getter
@Builder
public class TemplateResponse {

    private Long id;
    private Long merchantId;
    private String name;
    private String description;
    private BigDecimal originalPrice;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
