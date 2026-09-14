package com.lastcalleats.merchant.service;

import com.lastcalleats.merchant.dto.MerchantProfileRequest;
import com.lastcalleats.merchant.dto.MerchantProfileResponse;
import com.lastcalleats.common.exception.BusinessException;

/**
 * Service interface for merchant profile operations.
 * Include methods to get, update profile and return the public merchant page.
 */
public interface MerchantService {

    /**
     * Find merchant profile by ID.
     *
     * @param merchantId the ID of the merchant to look up
     * @return response DTO with merchant profile data
     * @throws BusinessException if merchant not found
     */
    MerchantProfileResponse getProfile(Long merchantId);

    /**
     * Update merchant name, address, and business hours.
     *
     * @param merchantId the ID of the merchant to update
     * @param request    new values for name, address, businessHours
     * @return response DTO with updated profile
     * @throws BusinessException if merchant not found
     */
    MerchantProfileResponse updateProfile(Long merchantId, MerchantProfileRequest request);

    /**
     * Return the public profile of a merchant by ID.
     * This endpoint can be accessed without login.
     *
     * @param merchantId the ID of the merchant
     * @return response DTO with merchant profile data
     * @throws BusinessException if merchant not found
     */
    MerchantProfileResponse getPublicProfile(Long merchantId);
}
