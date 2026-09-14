package com.lastcalleats.product.repository;

import com.lastcalleats.product.dto.UserBrowseProjection;
import com.lastcalleats.product.entity.ProductListingDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Repository interface for product listing database operations.
 * This repository provides CRUD operations for {@link ProductListingDO}
 * and custom query methods for retrieving listings by merchant,
 * availability, and creation time.
 */
@Repository
public interface ProductListingRepo extends JpaRepository<ProductListingDO, Long> {

    List<ProductListingDO> findByMerchantId(Long merchantId);

    List<ProductListingDO> findByMerchantIdAndIsAvailableTrue(Long merchantId);

    List<ProductListingDO> findAllByOrderByCreatedAtDesc();

    /**
     * Returns one stable page of purchasable listings with template and merchant data.
     *
     * <p>The inner query first reads one page in browse-index order. Only that small result is
     * joined to templates and merchants, so sorting does not make MySQL join every available
     * listing before applying the limit.</p>
     */
    @Query(value = """
        SELECT listing_page.id AS listingId,
               template.id AS templateId,
               merchant.id AS merchantId,
               merchant.name AS merchantName,
               merchant.address AS merchantAddress,
               template.name AS productName,
               template.description AS description,
               template.original_price AS originalPrice,
               listing_page.discount_price AS discountPrice,
               listing_page.remaining_quantity AS remainingQuantity,
               listing_page.pickup_start AS pickupStart,
               listing_page.pickup_end AS pickupEnd
        FROM (
          SELECT listing.id,
                 listing.template_id,
                 listing.merchant_id,
                 listing.discount_price,
                 listing.remaining_quantity,
                 listing.pickup_start,
                 listing.pickup_end,
                 listing.created_at
          FROM product_listing listing
          WHERE listing.is_available = TRUE
            AND listing.remaining_quantity > 0
          ORDER BY listing.created_at DESC, listing.id DESC
          LIMIT :limit OFFSET :offset
        ) listing_page
        JOIN product_template template ON template.id = listing_page.template_id
        JOIN merchant merchant ON merchant.id = listing_page.merchant_id
        ORDER BY listing_page.created_at DESC, listing_page.id DESC
        """, nativeQuery = true)
    List<UserBrowseProjection> findAvailableForBrowse(
        @Param("limit") int limit,
        @Param("offset") long offset
    );

    /**
     * Atomically reserves one unit when the listing is available and still has stock.
     *
     * <p>The availability check and decrement happen in one database statement, so concurrent
     * callers cannot all make a purchase decision from the same stale quantity. The update also
     * marks the listing unavailable when it reserves the last unit.</p>
     *
     * @param listingId listing to reserve
     * @return 1 when one unit was reserved; 0 when the listing was unavailable or sold out
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
        UPDATE product_listing
        SET is_available = CASE WHEN remaining_quantity = 1 THEN FALSE ELSE is_available END,
            remaining_quantity = remaining_quantity - 1
        WHERE id = :listingId
          AND is_available = TRUE
          AND remaining_quantity > 0
        """, nativeQuery = true)
    int reserveOneIfAvailable(@Param("listingId") Long listingId);
}
