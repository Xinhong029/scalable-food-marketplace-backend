package com.lastcalleats.product.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

/** Fields selected by the native browse query before they are exposed as an API response. */
public interface UserBrowseProjection {

  Long getListingId();

  Long getTemplateId();

  Long getMerchantId();

  String getMerchantName();

  String getMerchantAddress();

  String getProductName();

  String getDescription();

  BigDecimal getOriginalPrice();

  BigDecimal getDiscountPrice();

  Integer getRemainingQuantity();

  LocalTime getPickupStart();

  LocalTime getPickupEnd();
}
