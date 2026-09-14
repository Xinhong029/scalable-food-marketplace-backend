package com.lastcalleats.merchant.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Response DTO for the merchant dashboard, built by DashboardFacade.
 * Combine order stats and listing stats into one response for the frontend to show.
 */
@Getter
@Builder
public class MerchantDashboardResponse {

    private Integer todayOrderCount;    // number of orders placed today
    private BigDecimal todayRevenue;    // total revenue from PAID and COMPLETED orders today
    private Integer activeListingCount; // number of listings currently marked as available
}
