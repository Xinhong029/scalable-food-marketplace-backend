package com.lastcalleats.product.repository;

import com.lastcalleats.product.entity.ProductTemplateDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for product template database operations.
 * This repository provides CRUD operations for {@link ProductTemplateDO}
 * and custom query methods for retrieving templates by merchant.
 */
@Repository
public interface ProductTemplateRepo extends JpaRepository<ProductTemplateDO, Long> {

  /**
   * Finds all templates belonging to the specified merchant.
   * @param merchantId id of the merchant
   * @return list of templates owned by the merchant
   */
  List<ProductTemplateDO> findByMerchantId(Long merchantId);
}