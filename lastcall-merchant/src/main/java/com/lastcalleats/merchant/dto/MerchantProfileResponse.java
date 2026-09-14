package com.lastcalleats.merchant.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO for a merchant's profile, returned to the frontend after profile reads and updates.
 * Excludes sensitive fields such as the password hash; built from a MerchantDO entity via the service layer.
 */
@Getter
@Builder
public class MerchantProfileResponse {

    private Long id;
    private String email;
    private String name;
    private String address;
    private String businessHours;   // may be null if the merchant has not set their hours
    private Boolean isActive;       // false indicates the merchant account has been disabled
}
