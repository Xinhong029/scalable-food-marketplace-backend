package com.lastcalleats.product.provider;

import com.lastcalleats.product.entity.ProductListingDO;
import com.lastcalleats.product.repository.ProductListingRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ListingStatsProviderImpl.
 * Check that active listing count is returned correctly for different scenarios.
 */
@ExtendWith(MockitoExtension.class)
class ListingStatsProviderImplTest {

    @Mock
    private ProductListingRepo productListingRepo;

    @InjectMocks
    private ListingStatsProviderImpl listingStatsProvider;

    private ProductListingDO activeListing1;
    private ProductListingDO activeListing2;

    @BeforeEach
    void setUp() {
        activeListing1 = new ProductListingDO();
        activeListing1.setId(1L);
        activeListing1.setMerchantId(1L);
        activeListing1.setIsAvailable(true);

        activeListing2 = new ProductListingDO();
        activeListing2.setId(2L);
        activeListing2.setMerchantId(1L);
        activeListing2.setIsAvailable(true);
    }

    @Test
    void getActiveListingCount_twoActive_returnsTwo() {
        when(productListingRepo.findByMerchantIdAndIsAvailableTrue(1L))
                .thenReturn(List.of(activeListing1, activeListing2));

        int count = listingStatsProvider.getActiveListingCount(1L);

        assertEquals(2, count);
        verify(productListingRepo).findByMerchantIdAndIsAvailableTrue(1L);
    }

    @Test
    void getActiveListingCount_none_returnsZero() {
        when(productListingRepo.findByMerchantIdAndIsAvailableTrue(1L))
                .thenReturn(List.of());

        assertEquals(0, listingStatsProvider.getActiveListingCount(1L));
    }
}
