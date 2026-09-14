package com.lastcalleats.product.provider;

import com.lastcalleats.common.provider.ListingStatsProvider;
import com.lastcalleats.product.repository.ProductListingRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implementation of ListingStatsProvider in the product module.
 * Give the merchant dashboard the count of active listings.
 */
@Component
@RequiredArgsConstructor
public class ListingStatsProviderImpl implements ListingStatsProvider {

    private final ProductListingRepo productListingRepo;

    /**
     * Count listings with isAvailable = true for the given merchant.
     *
     * @param merchantId the ID of the merchant
     * @return number of active listings
     */
    @Override
    public int getActiveListingCount(Long merchantId) {
        return productListingRepo.findByMerchantIdAndIsAvailableTrue(merchantId).size();
    }
}
