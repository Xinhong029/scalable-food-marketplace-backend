package com.lastcalleats.user.service;

import com.lastcalleats.user.dto.FavoriteListingResponse;
import com.lastcalleats.common.exception.BusinessException;

import java.util.List;

/**
 * Service interface for managing user favorite listings.
 * Separated from UserService because it has its own logic and operates on the user_favorite table.
 */
public interface FavoriteService {

    /**
     * Add a listing to user's favorites.
     * Check listing exists and not already favorited before saving.
     *
     * @param userId    the ID of the user
     * @param listingId the ID of the listing to favorite
     * @throws BusinessException if listing not found or already favorited
     */
    void addFavorite(Long userId, Long listingId);

    /**
     * Remove a listing from user's favorites.
     *
     * @param userId    the ID of the user
     * @param listingId the ID of the listing to remove
     * @throws BusinessException if favorite record not found
     */
    void removeFavorite(Long userId, Long listingId);

    /**
     * Get all listings the user has favorited, with product and merchant info included.
     * If listing, template, or merchant data is missing, that item is skipped.
     *
     * @param userId the ID of the user
     * @return list of favorite listing responses, possibly empty
     */
    List<FavoriteListingResponse> listFavorites(Long userId);

    /**
     * Check if user has already favorited the given listing.
     *
     * @param userId    the ID of the user
     * @param listingId the ID of the listing
     * @return true if favorited, false otherwise
     */
    boolean isFavorite(Long userId, Long listingId);
}
