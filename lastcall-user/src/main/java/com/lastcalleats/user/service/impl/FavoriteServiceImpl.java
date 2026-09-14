package com.lastcalleats.user.service.impl;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import com.lastcalleats.merchant.entity.MerchantDO;
import com.lastcalleats.merchant.repository.MerchantRepo;
import com.lastcalleats.product.entity.ProductListingDO;
import com.lastcalleats.product.entity.ProductTemplateDO;
import com.lastcalleats.product.repository.ProductListingRepo;
import com.lastcalleats.product.repository.ProductTemplateRepo;
import com.lastcalleats.user.dto.FavoriteListingResponse;
import com.lastcalleats.user.entity.UserFavoriteDO;
import com.lastcalleats.user.repository.UserFavoriteRepo;
import com.lastcalleats.user.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of FavoriteService.
 * Manage user favorite listings and build response by joining listing, template, and merchant data.
 */
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final UserFavoriteRepo userFavoriteRepo;
    private final ProductListingRepo productListingRepo;
    private final ProductTemplateRepo productTemplateRepo;
    private final MerchantRepo merchantRepo;

    /**
     * Add a product listing to the user's favourites.
     * Check that the listing exists and is not already in the user's favourites before saving.
     *
     * @param userId    the ID of the user adding the favourite
     * @param listingId the ID of the listing to favourite
     * @throws BusinessException if the listing does not exist or is already favourited by this user
     */
    @Override
    public void addFavorite(Long userId, Long listingId) {
        // Verify the listing exists before creating the favourite record
        productListingRepo.findById(listingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Listing not found"));

        // Prevent duplicate favourites for the same user-listing pair
        if (userFavoriteRepo.existsByUserIdAndListingId(userId, listingId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Already favourited");
        }

        userFavoriteRepo.save(UserFavoriteDO.builder()
                .userId(userId)
                .listingId(listingId)
                .build());
    }

    /**
     * Remove a product listing from the user's favourites.
     *
     * @param userId    the ID of the user removing the favourite
     * @param listingId the ID of the listing to un-favourite
     * @throws BusinessException if no matching favourite record is found
     */
    @Override
    @Transactional
    public void removeFavorite(Long userId, Long listingId) {
        if (!userFavoriteRepo.existsByUserIdAndListingId(userId, listingId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Favourite not found");
        }
        userFavoriteRepo.deleteByUserIdAndListingId(userId, listingId);
    }

    /**
     * Return all listings the user has favourited, enriched with product and merchant details.
     * Any favourite record whose related listing, template, or merchant cannot be found is silently skipped.
     *
     * @param userId the ID of the user whose favourites to retrieve
     * @return a list of enriched favourite listing responses, possibly empty
     */
    @Override
    public List<FavoriteListingResponse> listFavorites(Long userId) {
        return userFavoriteRepo.findByUserId(userId).stream()
                .map(fav -> {
                    // skip if listing, template, or merchant is missing
                    ProductListingDO listing = productListingRepo.findById(fav.getListingId()).orElse(null);
                    if (listing == null) return null;
                    ProductTemplateDO template = productTemplateRepo.findById(listing.getTemplateId()).orElse(null);
                    if (template == null) return null;
                    MerchantDO merchant = merchantRepo.findById(listing.getMerchantId()).orElse(null);
                    if (merchant == null) return null;

                    // build response from listing, template, and merchant data
                    return FavoriteListingResponse.builder()
                            .listingId(listing.getId())
                            .merchantId(merchant.getId())
                            .merchantName(merchant.getName())
                            .productName(template.getName())
                            .description(template.getDescription())
                            .originalPrice(template.getOriginalPrice())
                            .discountPrice(listing.getDiscountPrice())
                            .remainingQuantity(listing.getRemainingQuantity())
                            .pickupStart(listing.getPickupStart())
                            .pickupEnd(listing.getPickupEnd())
                            .date(listing.getDate())
                            .isAvailable(listing.getIsAvailable())
                            .favoritedAt(fav.getCreatedAt())
                            .build();
                })
                .filter(r -> r != null)
                .collect(Collectors.toList());
    }

    /**
     * Check whether the user has already favourited the given listing.
     *
     * @param userId    the ID of the user to check
     * @param listingId the ID of the listing to check
     * @return true if a favourite record exists, false otherwise
     */
    @Override
    public boolean isFavorite(Long userId, Long listingId) {
        return userFavoriteRepo.existsByUserIdAndListingId(userId, listingId);
    }
}
