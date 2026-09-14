package com.lastcalleats.product.repository;

import com.lastcalleats.merchant.entity.MerchantDO;
import com.lastcalleats.product.dto.UserBrowseProjection;
import com.lastcalleats.product.entity.ProductListingDO;
import com.lastcalleats.product.entity.ProductTemplateDO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository tests for {@link ProductListingRepo}.
 *
 * <p>This class verifies that custom query methods return correct results.</p>
 */
@ExtendWith(SpringExtension.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ProductListingRepoTest {

  @Autowired
  private ProductListingRepo productListingRepo;

  @Autowired
  private TestEntityManager entityManager;

  /**
   * Minimal configuration for JPA test context.
   */
  @Configuration
  @EnableAutoConfiguration
  @EntityScan(basePackages = {
      "com.lastcalleats.product.entity",
      "com.lastcalleats.merchant.entity"
  })
  @EnableJpaRepositories("com.lastcalleats.product.repository")
  static class TestConfig {}

  @Test
  @DisplayName("findByMerchantId works correctly")
  void findByMerchantId_returnsCorrectResults() {
    productListingRepo.save(build(1L, true));
    productListingRepo.save(build(1L, false));
    productListingRepo.save(build(2L, true));

    List<ProductListingDO> result = productListingRepo.findByMerchantId(1L);

    assertEquals(2, result.size());
  }

  @Test
  @DisplayName("findByMerchantIdAndIsAvailableTrue works correctly")
  void findByMerchantIdAndIsAvailableTrue_returnsOnlyActive() {
    productListingRepo.save(build(1L, true));
    productListingRepo.save(build(1L, false));
    productListingRepo.save(build(1L, true));

    List<ProductListingDO> result =
        productListingRepo.findByMerchantIdAndIsAvailableTrue(1L);

    assertEquals(2, result.size());
    assertTrue(result.stream().allMatch(ProductListingDO::getIsAvailable));
  }

  @Test
  @DisplayName("findAllByOrderByCreatedAtDesc sorts correctly")
  void findAllByOrderByCreatedAtDesc_returnsSorted() {
    productListingRepo.save(buildWithTime(1L, true, LocalDateTime.now().minusHours(2)));
    productListingRepo.save(buildWithTime(1L, true, LocalDateTime.now().minusHours(1)));
    productListingRepo.save(buildWithTime(1L, true, LocalDateTime.now()));

    List<ProductListingDO> result =
        productListingRepo.findAllByOrderByCreatedAtDesc();

    assertEquals(3, result.size());
    assertTrue(result.get(0).getCreatedAt()
        .isAfter(result.get(1).getCreatedAt()));
  }

  @Test
  @DisplayName("findAvailableForBrowse joins related data, filters, and returns a stable slice")
  void findAvailableForBrowse_joinsFiltersAndPaginates() {
    MerchantDO merchant = entityManager.persist(MerchantDO.builder()
        .email("browse@example.com")
        .passwordHash("hash")
        .name("Sakura Sushi")
        .address("123 Main St")
        .isActive(true)
        .build());
    ProductTemplateDO template = entityManager.persist(ProductTemplateDO.builder()
        .merchantId(merchant.getId())
        .name("Bread")
        .description("Fresh bread")
        .originalPrice(new BigDecimal("15.99"))
        .isActive(true)
        .build());

    ProductListingDO older = buildWithTime(
        merchant.getId(), true, LocalDateTime.now().minusMinutes(1));
    older.setTemplateId(template.getId());
    older = productListingRepo.save(older);
    ProductListingDO newer = buildWithTime(merchant.getId(), true, LocalDateTime.now());
    newer.setTemplateId(template.getId());
    newer = productListingRepo.save(newer);
    ProductListingDO unavailable = build(merchant.getId(), false);
    unavailable.setTemplateId(template.getId());
    productListingRepo.save(unavailable);
    ProductListingDO soldOut = build(merchant.getId(), true);
    soldOut.setTemplateId(template.getId());
    soldOut.setRemainingQuantity(0);
    productListingRepo.save(soldOut);
    productListingRepo.flush();

    List<UserBrowseProjection> first = productListingRepo.findAvailableForBrowse(10, 0);
    List<UserBrowseProjection> second = productListingRepo.findAvailableForBrowse(1, 1);

    assertEquals(List.of(newer.getId(), older.getId()),
        first.stream().map(UserBrowseProjection::getListingId).toList());
    assertEquals(List.of(older.getId()),
        second.stream().map(UserBrowseProjection::getListingId).toList());
    UserBrowseProjection response = first.get(0);
    assertEquals(template.getId(), response.getTemplateId());
    assertEquals(merchant.getId(), response.getMerchantId());
    assertEquals("Sakura Sushi", response.getMerchantName());
    assertEquals("123 Main St", response.getMerchantAddress());
    assertEquals("Bread", response.getProductName());
    assertEquals("Fresh bread", response.getDescription());
    assertEquals(new BigDecimal("15.99"), response.getOriginalPrice());
  }

  @Test
  @DisplayName("atomic reservation decrements only while stock is available")
  void reserveOneIfAvailable_stopsAtZeroAndMarksUnavailable() {
    ProductListingDO listing = build(1L, true);
    listing.setQuantity(1);
    listing.setRemainingQuantity(1);
    listing = productListingRepo.saveAndFlush(listing);

    assertEquals(1, productListingRepo.reserveOneIfAvailable(listing.getId()));
    ProductListingDO soldOut = productListingRepo.findById(listing.getId()).orElseThrow();
    assertEquals(0, soldOut.getRemainingQuantity());
    assertFalse(soldOut.getIsAvailable());

    assertEquals(0, productListingRepo.reserveOneIfAvailable(listing.getId()));
    assertEquals(0, productListingRepo.findById(listing.getId()).orElseThrow()
        .getRemainingQuantity());
  }

  // -------- helper methods --------

  private ProductListingDO build(Long merchantId, Boolean available) {
    return buildWithTime(merchantId, available, LocalDateTime.now());
  }

  private ProductListingDO buildWithTime(
      Long merchantId,
      Boolean available,
      LocalDateTime createdAt
  ) {
    return ProductListingDO.builder()
        .merchantId(merchantId)
        .templateId(100L)
        .discountPrice(new BigDecimal("9.99"))
        .quantity(5)
        .remainingQuantity(5)
        .pickupStart(LocalTime.of(18, 0))
        .pickupEnd(LocalTime.of(20, 0))
        .date(LocalDate.now())
        .isAvailable(available)
        .createdAt(createdAt)
        .build();
  }
}
