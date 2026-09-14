package com.lastcalleats.product.service.impl;

import com.lastcalleats.product.dto.TemplateRequest;
import com.lastcalleats.product.dto.TemplateResponse;
import com.lastcalleats.product.entity.ProductTemplateDO;
import com.lastcalleats.product.repository.ProductTemplateRepo;
import com.lastcalleats.product.service.ProductTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for product template related business logic.
 *
 * This class handles:
 * - creating templates
 * - getting all templates for a merchant
 * - updating templates
 * - soft deleting templates
 */
@Service
@Transactional
public class ProductTemplateServiceImpl implements ProductTemplateService {

  /**
   * Repository for product template database operations.
   */
  private final ProductTemplateRepo productTemplateRepo;

  /**
   * Constructor injection.
   *
   * @param productTemplateRepo repository for product template table
   */
  public ProductTemplateServiceImpl(ProductTemplateRepo productTemplateRepo) {
    this.productTemplateRepo = productTemplateRepo;
  }

  /**
   * Create a new product template for the current merchant.
   *
   * @param merchantId current merchant id
   * @param request request body containing template data
   * @return created template response
   */
  @Override
  public TemplateResponse createTemplate(Long merchantId, TemplateRequest request) {
    ProductTemplateDO template = ProductTemplateDO.builder()
        .merchantId(merchantId)
        .name(request.getName())
        .description(request.getDescription())
        .originalPrice(request.getOriginalPrice())
        .isActive(true)
        .build();

    ProductTemplateDO savedTemplate = productTemplateRepo.save(template);
    return toResponse(savedTemplate);
  }

  /**
   * Get all templates belonging to the current merchant.
   *
   * @param merchantId current merchant id
   * @return list of template responses
   */
  @Override
  @Transactional(readOnly = true)
  public List<TemplateResponse> getTemplates(Long merchantId) {
    List<ProductTemplateDO> templates = productTemplateRepo.findByMerchantId(merchantId);

    return templates.stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  /**
   * Update an existing template.
   * First make sure the template exists and belongs to the current merchant.
   *
   * @param merchantId current merchant id
   * @param templateId template id to update
   * @param request updated template data
   * @return updated template response
   */
  @Override
  public TemplateResponse updateTemplate(Long merchantId, Long templateId, TemplateRequest request) {
    ProductTemplateDO template = getOwnedTemplateOrThrow(merchantId, templateId);

    template.setName(request.getName());
    template.setDescription(request.getDescription());
    template.setOriginalPrice(request.getOriginalPrice());
    template.setIsActive(true);

    ProductTemplateDO updatedTemplate = productTemplateRepo.save(template);
    return toResponse(updatedTemplate);
  }

  /**
   * Soft delete a template.
   * We do not remove the row from database.
   * We only mark it as inactive.
   *
   * @param merchantId current merchant id
   * @param templateId template id to delete
   */
  @Override
  public void deleteTemplate(Long merchantId, Long templateId) {
    ProductTemplateDO template = getOwnedTemplateOrThrow(merchantId, templateId);
    template.setIsActive(false);
    productTemplateRepo.save(template);
  }

  /**
   * Helper method:
   * Find a template by id and verify it belongs to the current merchant.
   *
   * @param merchantId current merchant id
   * @param templateId template id
   * @return owned template entity
   */
  private ProductTemplateDO getOwnedTemplateOrThrow(Long merchantId, Long templateId) {
    ProductTemplateDO template = productTemplateRepo.findById(templateId)
        .orElseThrow(() -> new RuntimeException("Template not found."));

    if (!template.getMerchantId().equals(merchantId)) {
      throw new RuntimeException("You cannot operate on another merchant's template.");
    }

    return template;
  }

  /**
   * Convert entity object to response DTO.
   *
   * @param template product template entity
   * @return template response DTO
   */
  private TemplateResponse toResponse(ProductTemplateDO template) {
    return TemplateResponse.builder()
        .id(template.getId())
        .merchantId(template.getMerchantId())
        .name(template.getName())
        .description(template.getDescription())
        .originalPrice(template.getOriginalPrice())
        .isActive(template.getIsActive())
        .createdAt(template.getCreatedAt())
        .build();
  }
}