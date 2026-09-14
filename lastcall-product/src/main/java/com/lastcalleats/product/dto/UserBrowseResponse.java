package com.lastcalleats.product.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Response DTO used when users browse available listings.
 *
 * This DTO contains listing information combined with merchant
 * and product details, designed for the user-facing browse page.
 */
@Getter
public class UserBrowseResponse {

    private Long listingId;
    private Long templateId;
    private Long merchantId;
    private String merchantName;
    private String merchantAddress;
    private String productName;
    private String description;
    private BigDecimal originalPrice;
    private BigDecimal discountPrice;
    private Integer remainingQuantity;
    private LocalTime pickupStart;
    private LocalTime pickupEnd;

    /** Creates a response from either a database projection or cached JSON. */
    @Builder
    @JsonCreator
    public UserBrowseResponse(
        @JsonProperty("listingId") Long listingId,
        @JsonProperty("templateId") Long templateId,
        @JsonProperty("merchantId") Long merchantId,
        @JsonProperty("merchantName") String merchantName,
        @JsonProperty("merchantAddress") String merchantAddress,
        @JsonProperty("productName") String productName,
        @JsonProperty("description") String description,
        @JsonProperty("originalPrice") BigDecimal originalPrice,
        @JsonProperty("discountPrice") BigDecimal discountPrice,
        @JsonProperty("remainingQuantity") Integer remainingQuantity,
        @JsonProperty("pickupStart") LocalTime pickupStart,
        @JsonProperty("pickupEnd") LocalTime pickupEnd) {
        this.listingId = listingId;
        this.templateId = templateId;
        this.merchantId = merchantId;
        this.merchantName = merchantName;
        this.merchantAddress = merchantAddress;
        this.productName = productName;
        this.description = description;
        this.originalPrice = originalPrice;
        this.discountPrice = discountPrice;
        this.remainingQuantity = remainingQuantity;
        this.pickupStart = pickupStart;
        this.pickupEnd = pickupEnd;
    }

    /** Converts the database projection into the public browse response. */
    public static UserBrowseResponse from(UserBrowseProjection projection) {
        return new UserBrowseResponse(
            projection.getListingId(),
            projection.getTemplateId(),
            projection.getMerchantId(),
            projection.getMerchantName(),
            projection.getMerchantAddress(),
            projection.getProductName(),
            projection.getDescription(),
            projection.getOriginalPrice(),
            projection.getDiscountPrice(),
            projection.getRemainingQuantity(),
            projection.getPickupStart(),
            projection.getPickupEnd()
        );
    }
}
