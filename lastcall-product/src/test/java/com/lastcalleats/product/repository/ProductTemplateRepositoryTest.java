package com.lastcalleats.product.repository;

import com.lastcalleats.product.entity.ProductTemplateDO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Repository tests for {@link ProductTemplateRepo}.
 *
 * <p>This test class verifies that custom query methods in
 * {@link ProductTemplateRepo} return the expected records.</p>
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never"
})
class ProductTemplateRepoTest {

  @Configuration
  @EnableAutoConfiguration
  @EntityScan(basePackages = {
      "com.lastcalleats.product.entity",
      "com.lastcalleats.merchant.entity"
  })
  @EnableJpaRepositories("com.lastcalleats.product.repository")
  static class TestConfig {
  }

  @Autowired
  private ProductTemplateRepo productTemplateRepo;

  /**
   * Verifies that {@code findByMerchantId} returns only templates
   * belonging to the specified merchant.
   */
  @Test
  @DisplayName("findByMerchantId should return only templates for the given merchant")
  void findByMerchantId_returnsOnlyTemplatesForMerchant() {
    productTemplateRepo.save(buildTemplate(1L, "Bread", true));
    productTemplateRepo.save(buildTemplate(1L, "Cake", false));
    productTemplateRepo.save(buildTemplate(2L, "Sushi", true));

    List<ProductTemplateDO> result = productTemplateRepo.findByMerchantId(1L);

    assertEquals(2, result.size());
    assertTrue(result.stream().allMatch(template -> template.getMerchantId().equals(1L)));
  }

  /**
   * Builds a {@link ProductTemplateDO} instance for repository testing.
   *
   * @param merchantId the merchant id
   * @param name the template name
   * @param isActive whether the template is active
   * @return a populated product template entity
   */
  private ProductTemplateDO buildTemplate(Long merchantId, String name, Boolean isActive) {
    return ProductTemplateDO.builder()
        .merchantId(merchantId)
        .name(name)
        .description(name + " description")
        .originalPrice(new BigDecimal("15.99"))
        .isActive(isActive)
        .build();
  }
}
