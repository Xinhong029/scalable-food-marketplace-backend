package com.lastcalleats.common.provider;

import java.math.BigDecimal;

/**
 * Interface for getting order statistics used in the merchant dashboard.
 * Put in common module so merchant can use it without depending on the order module directly.
 */
public interface OrderStatsProvider {

    /**
     * Get number of orders placed today for the given merchant.
     *
     * @param merchantId the ID of the merchant
     * @return count of today's orders
     */
    int getTodayOrderCount(Long merchantId);

    /**
     * Get total revenue from today's PAID and COMPLETED orders.
     *
     * @param merchantId the ID of the merchant
     * @return sum of order prices, only PAID and COMPLETED status counted
     */
    BigDecimal getTodayRevenue(Long merchantId);
}
