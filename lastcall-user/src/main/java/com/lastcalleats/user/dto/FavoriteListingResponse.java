package com.lastcalleats.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Response DTO for one item in the user's favorite listings.
 * Collect data from listing, product template, and merchant so frontend can show everything in one call.
 */
@Getter
@Builder
public class FavoriteListingResponse {

    private Long listingId;
    private Long merchantId;
    private String merchantName;
    private String productName;
    private String description;
    private BigDecimal originalPrice;   // price before discount, from the product template
    private BigDecimal discountPrice;   // actual price user pays for this listing
    private Integer remainingQuantity;
    private LocalTime pickupStart;
    private LocalTime pickupEnd;
    private LocalDate date;
    private Boolean isAvailable;        // false means merchant took this listing offline
    private LocalDateTime favoritedAt;  // when the user added this to favorites
}
