package com.lastcalleats.product.controller;

import com.lastcalleats.common.response.ApiResponse;
import com.lastcalleats.product.dto.TemplateRequest;
import com.lastcalleats.product.dto.TemplateResponse;
import com.lastcalleats.product.service.ProductTemplateService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for merchant product template APIs.
 *
 * <p>This controller provides endpoints for merchants to manage their
 * product templates. A product template represents the reusable base
 * information of a product, such as its name, description, and original price.</p>
 *
 * <p>This controller handles:
 * <ul>
 *   <li>creating a new template</li>
 *   <li>retrieving all templates for the current merchant</li>
 *   <li>updating an existing template</li>
 *   <li>soft deleting a template</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/merchant/templates")
public class ProductTemplateController {

  /**
   * Service for product template business logic.
   */
  private final ProductTemplateService productTemplateService;

  /**
   * Constructs the controller with the required template service.
   *
   * @param productTemplateService service for handling product template operations
   */
  public ProductTemplateController(ProductTemplateService productTemplateService) {
    this.productTemplateService = productTemplateService;
  }

  /**
   * Creates a new product template for the current merchant.
   *
   * @param merchantId current merchant id from authentication
   * @param request request body containing template information
   * @return success response containing the created template
   */
  @PostMapping
  public ApiResponse<TemplateResponse> createTemplate(
      @AuthenticationPrincipal Long merchantId,
      @Valid @RequestBody TemplateRequest request) {
    return ApiResponse.success(productTemplateService.createTemplate(merchantId, request));
  }

  /**
   * Retrieves all product templates belonging to the current merchant.
   *
   * @param merchantId current merchant id from authentication
   * @return success response containing the merchant's template list
   */
  @GetMapping
  public ApiResponse<List<TemplateResponse>> getTemplates(
      @AuthenticationPrincipal Long merchantId) {
    return ApiResponse.success(productTemplateService.getTemplates(merchantId));
  }

  /**
   * Updates an existing product template.
   *
   * @param merchantId current merchant id from authentication
   * @param templateId id of the template to update
   * @param request request body containing updated template information
   * @return success response containing the updated template
   */
  @PutMapping("/{id}")
  public ApiResponse<TemplateResponse> updateTemplate(
      @AuthenticationPrincipal Long merchantId,
      @PathVariable("id") Long templateId,
      @Valid @RequestBody TemplateRequest request) {
    return ApiResponse.success(productTemplateService.updateTemplate(merchantId, templateId, request));
  }

  /**
   * Soft deletes a product template by marking it inactive.
   *
   * @param merchantId current merchant id from authentication
   * @param templateId id of the template to delete
   * @return success response with no data
   */
  @DeleteMapping("/{id}")
  public ApiResponse<Void> deleteTemplate(
      @AuthenticationPrincipal Long merchantId,
      @PathVariable("id") Long templateId) {
    productTemplateService.deleteTemplate(merchantId, templateId);
    return ApiResponse.success();
  }
}