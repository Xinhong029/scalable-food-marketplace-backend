package com.lastcalleats.user.repository;

import com.lastcalleats.user.entity.UserFavoriteDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for UserFavoriteDO.
 * Has query and delete methods for managing the user-to-listing favorite records.
 */
@Repository
public interface UserFavoriteRepo extends JpaRepository<UserFavoriteDO, Long> {

    /**
     * Get all favorite records for the given user.
     *
     * @param userId the ID of the user
     * @return list of favorite records, possibly empty
     */
    List<UserFavoriteDO> findByUserId(Long userId);

    /**
     * Find one favorite record by user and listing.
     *
     * @param userId    the ID of the user
     * @param listingId the ID of the listing
     * @return Optional with the record, or empty if not found
     */
    Optional<UserFavoriteDO> findByUserIdAndListingId(Long userId, Long listingId);

    /**
     * Check if user has already favorited the listing.
     *
     * @param userId    the ID of the user
     * @param listingId the ID of the listing
     * @return true if favorite record exists, false otherwise
     */
    boolean existsByUserIdAndListingId(Long userId, Long listingId);

    /**
     * Delete the favorite record for user and listing.
     * Caller must have @Transactional annotation.
     *
     * @param userId    the ID of the user
     * @param listingId the ID of the listing
     */
    void deleteByUserIdAndListingId(Long userId, Long listingId);
}
