package com.lastcalleats.product.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Response DTO representing a product listing.
 *
 * A listing is created by a merchant based on a product template.
 * It includes pricing, quantity, availability, and pickup time details.
 */

@Getter
@Builder
public class ListingResponse {

    private Long id;
    private Long merchantId;
    private Long templateId;
    private String templateName;
    private BigDecimal originalPrice;
    private BigDecimal discountPrice;
    private Integer quantity;
    private Integer remainingQuantity;
    private LocalTime pickupStart;
    private LocalTime pickupEnd;
    private LocalDate date;
    private Boolean isAvailable;
}
