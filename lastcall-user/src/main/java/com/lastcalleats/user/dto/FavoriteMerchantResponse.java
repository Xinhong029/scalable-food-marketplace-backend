package com.lastcalleats.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Response DTO for an item in the user's favourite merchants list.
 * Contains basic merchant identification and the timestamp when the user added the merchant to favourites.
 */
@Getter
@Builder
public class FavoriteMerchantResponse {

    private Long merchantId;
    private String merchantName;
    private String merchantAddress;
    private LocalDateTime favoritedAt;  // timestamp when the user favourited this merchant
}
