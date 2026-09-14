package com.lastcalleats.product.service.impl;

import com.lastcalleats.product.dto.TemplateRequest;
import com.lastcalleats.product.dto.TemplateResponse;
import com.lastcalleats.product.entity.ProductTemplateDO;
import com.lastcalleats.product.repository.ProductTemplateRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ProductTemplateServiceImpl}.
 *
 * <p>This test class verifies the business logic for creating templates,
 * retrieving templates for a merchant, updating templates, and soft deleting
 * templates.</p>
 *
 * <p>Repository dependencies are mocked so these tests focus only on
 * service-layer behavior.</p>
 */
@ExtendWith(MockitoExtension.class)
class ProductTemplateServiceImplTest {

  @Mock
  private ProductTemplateRepo productTemplateRepo;

  @InjectMocks
  private ProductTemplateServiceImpl productTemplateService;

  private Long merchantId;
  private TemplateRequest templateRequest;

  /**
   * Initializes common test data before each test.
   */
  @BeforeEach
  void setUp() {
    merchantId = 1L;

    templateRequest = new TemplateRequest();
    templateRequest.setName("Bread");
    templateRequest.setDescription("Fresh bread");
    templateRequest.setOriginalPrice(new BigDecimal("15.99"));
  }

  /**
   * Verifies that creating a template succeeds and marks the template active
   * by default.
   */
  @Test
  @DisplayName("createTemplate should create and return a template response")
  void createTemplate_validRequest_returnsCreatedTemplate() {
    ProductTemplateDO savedTemplate = ProductTemplateDO.builder()
        .id(100L)
        .merchantId(merchantId)
        .name("Bread")
        .description("Fresh bread")
        .originalPrice(new BigDecimal("15.99"))
        .isActive(true)
        .createdAt(LocalDateTime.of(2026, 4, 21, 15, 0))
        .build();

    when(productTemplateRepo.save(any(ProductTemplateDO.class))).thenReturn(savedTemplate);

    TemplateResponse response = productTemplateService.createTemplate(merchantId, templateRequest);

    assertNotNull(response);
    assertEquals(100L, response.getId());
    assertEquals(merchantId, response.getMerchantId());
    assertEquals("Bread", response.getName());
    assertEquals("Fresh bread", response.getDescription());
    assertEquals(new BigDecimal("15.99"), response.getOriginalPrice());
    assertTrue(response.getIsActive());

    ArgumentCaptor<ProductTemplateDO> captor = ArgumentCaptor.forClass(ProductTemplateDO.class);
    verify(productTemplateRepo).save(captor.capture());

    ProductTemplateDO toSave = captor.getValue();
    assertEquals(merchantId, toSave.getMerchantId());
    assertEquals("Bread", toSave.getName());
    assertEquals("Fresh bread", toSave.getDescription());
    assertEquals(new BigDecimal("15.99"), toSave.getOriginalPrice());
    assertTrue(toSave.getIsActive());
  }

  /**
   * Verifies that getting templates returns all templates belonging to the
   * specified merchant mapped into response DTOs.
   */
  @Test
  @DisplayName("getTemplates should return mapped templates for merchant")
  void getTemplates_returnsMerchantTemplates() {
    ProductTemplateDO template1 = ProductTemplateDO.builder()
        .id(1L)
        .merchantId(merchantId)
        .name("Bread")
        .description("Fresh bread")
        .originalPrice(new BigDecimal("15.99"))
        .isActive(true)
        .createdAt(LocalDateTime.of(2026, 4, 21, 10, 0))
        .build();

    ProductTemplateDO template2 = ProductTemplateDO.builder()
        .id(2L)
        .merchantId(merchantId)
        .name("Cake")
        .description("Chocolate cake")
        .originalPrice(new BigDecimal("22.50"))
        .isActive(false)
        .createdAt(LocalDateTime.of(2026, 4, 21, 11, 0))
        .build();

    when(productTemplateRepo.findByMerchantId(merchantId)).thenReturn(List.of(template1, template2));

    List<TemplateResponse> result = productTemplateService.getTemplates(merchantId);

    assertEquals(2, result.size());

    TemplateResponse first = result.get(0);
    assertEquals(1L, first.getId());
    assertEquals("Bread", first.getName());
    assertTrue(first.getIsActive());

    TemplateResponse second = result.get(1);
    assertEquals(2L, second.getId());
    assertEquals("Cake", second.getName());
    assertFalse(second.getIsActive());
  }

  /**
   * Verifies that updating an owned template changes its fields and returns
   * the updated response.
   */
  @Test
  @DisplayName("updateTemplate should update owned template and return response")
  void updateTemplate_ownedTemplate_updatesFieldsAndReturnsResponse() {
    ProductTemplateDO existingTemplate = ProductTemplateDO.builder()
        .id(10L)
        .merchantId(merchantId)
        .name("Old Bread")
        .description("Old description")
        .originalPrice(new BigDecimal("10.00"))
        .isActive(false)
        .createdAt(LocalDateTime.of(2026, 4, 21, 9, 0))
        .build();

    TemplateRequest updateRequest = new TemplateRequest();
    updateRequest.setName("Updated Bread");
    updateRequest.setDescription("Updated description");
    updateRequest.setOriginalPrice(new BigDecimal("12.99"));

    ProductTemplateDO updatedTemplate = ProductTemplateDO.builder()
        .id(10L)
        .merchantId(merchantId)
        .name("Updated Bread")
        .description("Updated description")
        .originalPrice(new BigDecimal("12.99"))
        .isActive(true)
        .createdAt(LocalDateTime.of(2026, 4, 21, 9, 0))
        .build();

    when(productTemplateRepo.findById(10L)).thenReturn(Optional.of(existingTemplate));
    when(productTemplateRepo.save(existingTemplate)).thenReturn(updatedTemplate);

    TemplateResponse response = productTemplateService.updateTemplate(merchantId, 10L, updateRequest);

    assertNotNull(response);
    assertEquals(10L, response.getId());
    assertEquals("Updated Bread", response.getName());
    assertEquals("Updated description", response.getDescription());
    assertEquals(new BigDecimal("12.99"), response.getOriginalPrice());
    assertTrue(response.getIsActive());

    assertEquals("Updated Bread", existingTemplate.getName());
    assertEquals("Updated description", existingTemplate.getDescription());
    assertEquals(new BigDecimal("12.99"), existingTemplate.getOriginalPrice());
    assertTrue(existingTemplate.getIsActive());

    verify(productTemplateRepo).save(existingTemplate);
  }

  /**
   * Verifies that updating a missing template throws an exception.
   */
  @Test
  @DisplayName("updateTemplate should throw when template is missing")
  void updateTemplate_templateNotFound_throwsException() {
    when(productTemplateRepo.findById(10L)).thenReturn(Optional.empty());

    RuntimeException ex = assertThrows(RuntimeException.class,
        () -> productTemplateService.updateTemplate(merchantId, 10L, templateRequest));

    assertEquals("Template not found.", ex.getMessage());
    verify(productTemplateRepo, never()).save(any());
  }

  /**
   * Verifies that updating another merchant's template throws an exception.
   */
  @Test
  @DisplayName("updateTemplate should throw when template belongs to another merchant")
  void updateTemplate_templateOwnedByAnotherMerchant_throwsException() {
    ProductTemplateDO otherMerchantTemplate = ProductTemplateDO.builder()
        .id(10L)
        .merchantId(999L)
        .name("Bread")
        .description("Fresh bread")
        .originalPrice(new BigDecimal("15.99"))
        .isActive(true)
        .build();

    when(productTemplateRepo.findById(10L)).thenReturn(Optional.of(otherMerchantTemplate));

    RuntimeException ex = assertThrows(RuntimeException.class,
        () -> productTemplateService.updateTemplate(merchantId, 10L, templateRequest));

    assertEquals("You cannot operate on another merchant's template.", ex.getMessage());
    verify(productTemplateRepo, never()).save(any());
  }

  /**
   * Verifies that deleting an owned template performs a soft delete by marking
   * the template inactive and saving it.
   */
  @Test
  @DisplayName("deleteTemplate should mark owned template inactive")
  void deleteTemplate_ownedTemplate_marksInactiveAndSaves() {
    ProductTemplateDO existingTemplate = ProductTemplateDO.builder()
        .id(10L)
        .merchantId(merchantId)
        .name("Bread")
        .description("Fresh bread")
        .originalPrice(new BigDecimal("15.99"))
        .isActive(true)
        .build();

    when(productTemplateRepo.findById(10L)).thenReturn(Optional.of(existingTemplate));

    productTemplateService.deleteTemplate(merchantId, 10L);

    assertFalse(existingTemplate.getIsActive());
    verify(productTemplateRepo).save(existingTemplate);
  }

  /**
   * Verifies that deleting a missing template throws an exception.
   */
  @Test
  @DisplayName("deleteTemplate should throw when template is missing")
  void deleteTemplate_templateNotFound_throwsException() {
    when(productTemplateRepo.findById(10L)).thenReturn(Optional.empty());

    RuntimeException ex = assertThrows(RuntimeException.class,
        () -> productTemplateService.deleteTemplate(merchantId, 10L));

    assertEquals("Template not found.", ex.getMessage());
    verify(productTemplateRepo, never()).save(any());
  }

  /**
   * Verifies that deleting another merchant's template throws an exception.
   */
  @Test
  @DisplayName("deleteTemplate should throw when template belongs to another merchant")
  void deleteTemplate_templateOwnedByAnotherMerchant_throwsException() {
    ProductTemplateDO otherMerchantTemplate = ProductTemplateDO.builder()
        .id(10L)
        .merchantId(999L)
        .name("Bread")
        .description("Fresh bread")
        .originalPrice(new BigDecimal("15.99"))
        .isActive(true)
        .build();

    when(productTemplateRepo.findById(10L)).thenReturn(Optional.of(otherMerchantTemplate));

    RuntimeException ex = assertThrows(RuntimeException.class,
        () -> productTemplateService.deleteTemplate(merchantId, 10L));

    assertEquals("You cannot operate on another merchant's template.", ex.getMessage());
    verify(productTemplateRepo, never()).save(any());
  }
}
