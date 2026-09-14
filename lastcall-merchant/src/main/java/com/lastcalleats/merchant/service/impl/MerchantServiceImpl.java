package com.lastcalleats.merchant.service.impl;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import com.lastcalleats.merchant.dto.MerchantProfileRequest;
import com.lastcalleats.merchant.dto.MerchantProfileResponse;
import com.lastcalleats.merchant.entity.MerchantDO;
import com.lastcalleats.merchant.repository.MerchantRepo;
import com.lastcalleats.merchant.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Implementation of MerchantService.
 * Get and update merchant profile data from database.
 */
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantRepo merchantRepo;

    /**
     * Find merchant by ID and return profile data.
     *
     * @param merchantId the ID of the merchant to look up
     * @return response DTO with merchant profile
     * @throws BusinessException if merchant not found
     */
    @Override
    public MerchantProfileResponse getProfile(Long merchantId) {
        MerchantDO merchant = findMerchantById(merchantId);
        return toResponse(merchant);
    }

    /**
     * Update merchant name, address, and business hours, then save to database.
     *
     * @param merchantId the ID of the merchant to update
     * @param request    new values to apply
     * @return response DTO with updated profile
     * @throws BusinessException if merchant not found
     */
    @Override
    public MerchantProfileResponse updateProfile(Long merchantId, MerchantProfileRequest request) {
        MerchantDO merchant = findMerchantById(merchantId);
        merchant.setName(request.getName());
        merchant.setAddress(request.getAddress());
        merchant.setBusinessHours(request.getBusinessHours());
        merchantRepo.save(merchant);
        return toResponse(merchant);
    }

    /**
     * Return public profile of a merchant by ID.
     * Same data as getProfile, but this endpoint is open to everyone.
     *
     * @param merchantId the ID of the merchant
     * @return response DTO with merchant profile
     * @throws BusinessException if merchant not found
     */
    @Override
    public MerchantProfileResponse getPublicProfile(Long merchantId) {
        MerchantDO merchant = findMerchantById(merchantId);
        return toResponse(merchant);
    }

    // Fetch merchant by ID; throw BusinessException if not found
    private MerchantDO findMerchantById(Long merchantId) {
        return merchantRepo.findById(merchantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MERCHANT_NOT_FOUND));
    }

    // Map MerchantDO to response DTO
    private MerchantProfileResponse toResponse(MerchantDO merchant) {
        return MerchantProfileResponse.builder()
                .id(merchant.getId())
                .email(merchant.getEmail())
                .name(merchant.getName())
                .address(merchant.getAddress())
                .businessHours(merchant.getBusinessHours())
                .isActive(merchant.getIsActive())
                .build();
    }
}
