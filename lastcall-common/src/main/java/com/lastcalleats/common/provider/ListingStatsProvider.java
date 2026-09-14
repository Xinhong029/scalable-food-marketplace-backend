package com.lastcalleats.common.provider;

/**
 * Interface for getting product listing statistics used in the merchant dashboard.
 * Put in common module so merchant can use it without depending on the product module directly.
 */
public interface ListingStatsProvider {

    /**
     * Get number of active listings for the given merchant.
     *
     * @param merchantId the ID of the merchant
     * @return count of listings with isAvailable = true
     */
    int getActiveListingCount(Long merchantId);
}
