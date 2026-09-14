package com.lastcalleats.review.repository;

import com.lastcalleats.review.entity.ReviewDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access interface for {@link com.lastcalleats.review.entity.ReviewDO}.
 * The {@code existsByOrderId} method is used by the factory to enforce the
 * one-review-per-order constraint before attempting to persist a new record.
 */
@Repository
public interface ReviewRepo extends JpaRepository<ReviewDO, Long> {

    Optional<ReviewDO> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    Page<ReviewDO> findByMerchantIdAndIsVisibleTrueOrderByCreatedAtDesc(Long merchantId, Pageable pageable);

    Page<ReviewDO> findByTemplateIdAndIsVisibleTrueOrderByCreatedAtDesc(Long templateId, Pageable pageable);
}
