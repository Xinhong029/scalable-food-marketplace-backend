package com.lastcalleats.merchant.facade;

import com.lastcalleats.common.provider.ListingStatsProvider;
import com.lastcalleats.common.provider.OrderStatsProvider;
import com.lastcalleats.merchant.dto.MerchantDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Facade for the merchant dashboard (Facade Pattern).
 * Collect order and listing statistics from multiple modules into a single response,
 * so the controller only needs to call one method without knowing which modules are involved.
 */
@Component
@RequiredArgsConstructor
public class DashboardFacade {

    private final OrderStatsProvider orderStatsProvider;
    private final ListingStatsProvider listingStatsProvider;

    /**
     * Get dashboard data for the given merchant.
     *
     * @param merchantId the merchant's ID
     * @return a response containing today's order count, today's revenue, and active listing count
     */
    public MerchantDashboardResponse getDashboard(Long merchantId) {
        return MerchantDashboardResponse.builder()
                // get order stats from the order module via provider interface
                .todayOrderCount(orderStatsProvider.getTodayOrderCount(merchantId))
                .todayRevenue(orderStatsProvider.getTodayRevenue(merchantId))
                // get listing stats from the product module via provider interface
                .activeListingCount(listingStatsProvider.getActiveListingCount(merchantId))
                .build();
    }
}
