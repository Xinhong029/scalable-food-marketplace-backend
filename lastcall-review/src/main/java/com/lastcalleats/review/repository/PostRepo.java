package com.lastcalleats.review.repository;

import com.lastcalleats.review.entity.PostDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access interface for {@link com.lastcalleats.review.entity.PostDO}.
 * All query methods filter by {@code isVisible = true} so soft-deleted posts
 * are never returned to the application layer.
 */
@Repository
public interface PostRepo extends JpaRepository<PostDO, Long> {

    Page<PostDO> findByUserIdAndIsVisibleTrueOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<PostDO> findByMerchantIdAndIsVisibleTrueOrderByCreatedAtDesc(Long merchantId, Pageable pageable);

    Page<PostDO> findByIsVisibleTrueOrderByCreatedAtDesc(Pageable pageable);
}
