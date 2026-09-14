package com.lastcalleats.order.repository;

import com.lastcalleats.order.entity.PickupCodeDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Provides persistence access for {@link PickupCodeDO} records. The repository includes lookup
 * methods used by customer pickup retrieval and merchant-side verification flows.
 */
@Repository
public interface PickupCodeRepo extends JpaRepository<PickupCodeDO, Long> {

  /**
   * Finds a pickup code by order ID.
   *
   * @param orderId order ID
   * @return the pickup code if it exists, otherwise empty
   */
  Optional<PickupCodeDO> findByOrderId(Long orderId);

  /**
   * Finds a pickup code by merchant ID and numeric code. Uses a custom JPQL query.
   *
   * @param merchantId  merchant ID
   * @param numericCode numeric pickup code
   * @return the pickup code if it exists, otherwise empty
   */
  @Query("""
      select pc from PickupCodeDO pc
      join OrderDO o on o.id = pc.orderId
      where o.merchantId = :merchantId and pc.numericCode = :numericCode
      """)
  // Find this numeric code in the current merchant's orders.
  Optional<PickupCodeDO> findByMerchantIdAndNumericCode(Long merchantId, String numericCode);

  /**
   * Finds a pickup code by merchant ID and QR string.
   *
   * @param merchantId merchant ID
   * @param qrCode     QR string
   * @return the pickup code if it exists, otherwise empty
   */
  @Query("""
      select pc from PickupCodeDO pc
      join OrderDO o on o.id = pc.orderId
      where o.merchantId = :merchantId and pc.qrCode = :qrCode
      """)
  Optional<PickupCodeDO> findByMerchantIdAndQrCode(Long merchantId, String qrCode);
}
