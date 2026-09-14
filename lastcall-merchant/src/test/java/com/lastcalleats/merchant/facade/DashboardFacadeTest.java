package com.lastcalleats.merchant.facade;

import com.lastcalleats.common.provider.ListingStatsProvider;
import com.lastcalleats.common.provider.OrderStatsProvider;
import com.lastcalleats.merchant.dto.MerchantDashboardResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DashboardFacade.
 * Check that getDashboard correctly collect data from both order and listing providers.
 */
@ExtendWith(MockitoExtension.class)
class DashboardFacadeTest {

    @Mock
    private OrderStatsProvider orderStatsProvider;

    @Mock
    private ListingStatsProvider listingStatsProvider;

    @InjectMocks
    private DashboardFacade dashboardFacade;

    @Test
    void getDashboard_aggregatesAllProviders() {
        when(orderStatsProvider.getTodayOrderCount(1L)).thenReturn(5);
        when(orderStatsProvider.getTodayRevenue(1L)).thenReturn(new BigDecimal("120.50"));
        when(listingStatsProvider.getActiveListingCount(1L)).thenReturn(3);

        MerchantDashboardResponse response = dashboardFacade.getDashboard(1L);

        assertNotNull(response);
        assertEquals(5, response.getTodayOrderCount());
        assertEquals(new BigDecimal("120.50"), response.getTodayRevenue());
        assertEquals(3, response.getActiveListingCount());
        verify(orderStatsProvider).getTodayOrderCount(1L);
        verify(orderStatsProvider).getTodayRevenue(1L);
        verify(listingStatsProvider).getActiveListingCount(1L);
    }

    @Test
    void getDashboard_noData_returnsZeros() {
        when(orderStatsProvider.getTodayOrderCount(2L)).thenReturn(0);
        when(orderStatsProvider.getTodayRevenue(2L)).thenReturn(BigDecimal.ZERO);
        when(listingStatsProvider.getActiveListingCount(2L)).thenReturn(0);

        MerchantDashboardResponse response = dashboardFacade.getDashboard(2L);

        assertEquals(0, response.getTodayOrderCount());
        assertEquals(BigDecimal.ZERO, response.getTodayRevenue());
        assertEquals(0, response.getActiveListingCount());
    }
}
