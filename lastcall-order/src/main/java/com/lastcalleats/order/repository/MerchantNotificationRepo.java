package com.lastcalleats.order.repository;

import com.lastcalleats.order.entity.MerchantNotificationDO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Persistence operations for merchant notifications. */
@Repository
public interface MerchantNotificationRepo extends JpaRepository<MerchantNotificationDO, Long> {

  List<MerchantNotificationDO> findByMerchantIdOrderByCreatedAtDesc(Long merchantId);

  Optional<MerchantNotificationDO> findByIdAndMerchantId(Long id, Long merchantId);

  long countByEventId(String eventId);
}
