package com.lastcalleats.order.repository;

import com.lastcalleats.order.entity.OrderDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Provides persistence access for {@link OrderDO} records. The repository adds query methods that
 * support user order history, merchant views, and duplicate-order checks.
 */
@Repository
public interface OrderRepo extends JpaRepository<OrderDO, Long> {
  // JpaRepository gives basic database methods
  // like save, findById, deleteById, findAll, and count.

  /**
   * Finds orders by user ID.
   *
   * @param userId user ID
   * @return all orders of the user
   */
  List<OrderDO> findByUserId(Long userId);

  /**
   * Finds an order by order ID and user ID.
   *
   * @param id     order ID
   * @param userId user ID
   * @return the order if it exists and belongs to the user, otherwise empty
   */
  Optional<OrderDO> findByIdAndUserId(Long id, Long userId);

  /**
   * Finds orders by merchant ID.
   *
   * @param merchantId merchant ID
   * @return all orders of the merchant
   */
  List<OrderDO> findByMerchantId(Long merchantId);

  /**
   * Checks whether the user has already placed an order in the given time range. The same user can
   * buy the same listing only once per day.
   *
   * @param userId    user ID
   * @param listingId listing ID
   * @param start     start time
   * @param end       end time
   * @return true if a matching order exists
   */
  boolean existsByUserIdAndListingIdAndCreatedAtBetween(Long userId, Long listingId,
      LocalDateTime start, LocalDateTime end);
}
