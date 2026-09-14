package com.lastcalleats.merchant.controller;

import com.lastcalleats.common.response.ApiResponse;
import com.lastcalleats.merchant.dto.MerchantDashboardResponse;
import com.lastcalleats.merchant.dto.MerchantProfileRequest;
import com.lastcalleats.merchant.dto.MerchantProfileResponse;
import com.lastcalleats.merchant.facade.DashboardFacade;
import com.lastcalleats.merchant.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for merchant-related operations.
 * Handle profile management, dashboard, and public merchant page endpoints.
 */
@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;
    // Facade aggregates data from multiple modules for the dashboard endpoint
    private final DashboardFacade dashboardFacade;

    /**
     * Get the profile of the current logged-in merchant.
     *
     * @return merchant profile data
     */
    @GetMapping("/profile")
    public ApiResponse<MerchantProfileResponse> getProfile() {
        Long merchantId = getCurrentMerchantId();
        return ApiResponse.success(merchantService.getProfile(merchantId));
    }

    /**
     * Update the current merchant's profile with new data.
     *
     * @param request new values for name, address, businessHours; validated by @Valid
     * @return updated merchant profile
     */
    @PutMapping("/profile")
    public ApiResponse<MerchantProfileResponse> updateProfile(
            @Valid @RequestBody MerchantProfileRequest request) {
        Long merchantId = getCurrentMerchantId();
        return ApiResponse.success(merchantService.updateProfile(merchantId, request));
    }

    /**
     * Get dashboard data for the current merchant.
     * Includes today's order count, revenue, and active listing count from DashboardFacade.
     *
     * @return dashboard summary with order and listing stats
     */
    @GetMapping("/dashboard")
    public ApiResponse<MerchantDashboardResponse> getDashboard() {
        Long merchantId = getCurrentMerchantId();
        return ApiResponse.success(dashboardFacade.getDashboard(merchantId));
    }

    /**
     * Get public profile of a merchant by ID.
     * This endpoint can be accessed without login.
     *
     * @param id the ID of the merchant
     * @return merchant profile data
     */
    @GetMapping("/{id}")
    public ApiResponse<MerchantProfileResponse> getPublicProfile(@PathVariable Long id) {
        return ApiResponse.success(merchantService.getPublicProfile(id));
    }

    // Extract the authenticated merchant ID from the SecurityContext
    private Long getCurrentMerchantId() {
        String merchantId = SecurityContextHolder.getContext().getAuthentication().getName();
        return Long.parseLong(merchantId);
    }
}
