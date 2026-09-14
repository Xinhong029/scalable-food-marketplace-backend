package com.lastcalleats.product.service;

import com.lastcalleats.product.dto.TemplateRequest;
import com.lastcalleats.product.dto.TemplateResponse;

import java.util.List;

/**
 * Service interface for product template related business logic.
 *
 * A product template is a reusable food definition created by a merchant.
 * Example:
 * - name
 * - description
 * - original price
 *
 * This service defines the core operations for managing templates.
 */
public interface ProductTemplateService {

  /**
   * Create a new product template for the given merchant.
   *
   * @param merchantId the current merchant's id
   * @param request request body containing template data
   * @return created template response
   */
  TemplateResponse createTemplate(Long merchantId, TemplateRequest request);

  /**
   * Get all templates belonging to the given merchant.
   *
   * @param merchantId the current merchant's id
   * @return list of template responses
   */
  List<TemplateResponse> getTemplates(Long merchantId);

  /**
   * Update an existing template owned by the given merchant.
   *
   * @param merchantId the current merchant's id
   * @param templateId id of the template to update
   * @param request request body containing updated template data
   * @return updated template response
   */
  TemplateResponse updateTemplate(Long merchantId, Long templateId, TemplateRequest request);

  /**
   * Soft delete a template owned by the given merchant.
   * Usually this means setting isActive = false.
   *
   * @param merchantId the current merchant's id
   * @param templateId id of the template to delete
   */
  void deleteTemplate(Long merchantId, Long templateId);
}