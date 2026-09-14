package com.lastcalleats.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for updating a merchant's profile via PUT /api/merchant/profile.
 * Contains only the fields that a merchant is allowed to modify; immutable fields such as email are excluded.
 */
@Data
public class MerchantProfileRequest {

    // Store name; must be 2–100 characters
    @NotBlank
    @Size(min = 2, max = 100)
    private String name;

    // Physical address of the store
    @NotBlank
    @Size(max = 500)
    private String address;

    // Human-readable business hours string (e.g. "09:00–21:00"); optional
    @Size(max = 100)
    private String businessHours;
}
