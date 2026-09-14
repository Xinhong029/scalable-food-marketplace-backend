package com.lastcalleats.product.controller;

import com.lastcalleats.common.response.ApiResponse;
import com.lastcalleats.common.response.SliceResult;
import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import com.lastcalleats.product.dto.ListingRequest;
import com.lastcalleats.product.dto.ListingResponse;
import com.lastcalleats.product.dto.UserBrowseResponse;
import com.lastcalleats.product.service.ProductListingService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;

import java.util.List;

/**
 * Controller for merchant product listing APIs.
 *
 * This controller handles:
 * - create listing
 * - get all listings for the current merchant
 * - deactivate listing
 * - browse available listings for users
 */
@RestController
public class ProductListingController {

  private final ProductListingService productListingService;

  public ProductListingController(ProductListingService productListingService) {
    this.productListingService = productListingService;
  }

  /**
   * Browse one zero-based page of available listings (no auth required).
   *
   * @param page zero-based page number
   * @param size number of listings, from 1 through 100
   * @return listing content and whether another page exists
   */
  @GetMapping("/api/products/browse")
  public ApiResponse<SliceResult<UserBrowseResponse>> browseListings(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    if (page < 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "page must be at least 0");
    }
    if (size < 1 || size > 100) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "size must be between 1 and 100");
    }
    return ApiResponse.success(SliceResult.of(
        productListingService.browseAvailableListings(PageRequest.of(page, size))));
  }

  /**
   * Create a new listing.
   *
   * @param merchantId current merchant from JWT
   * @param request listing request body
   * @return created listing response
   */
  @PostMapping("/api/merchant/listings")
  public ApiResponse<ListingResponse> createListing(
      @AuthenticationPrincipal Long merchantId,
      @Valid @RequestBody ListingRequest request) {
    return ApiResponse.success(productListingService.createListing(merchantId, request));
  }

  /**
   * Get all listings for the current merchant.
   *
   * @param merchantId current merchant from JWT
   * @return list of listing responses
   */
  @GetMapping("/api/merchant/listings")
  public ApiResponse<List<ListingResponse>> getMerchantListings(
      @AuthenticationPrincipal Long merchantId) {
    return ApiResponse.success(productListingService.getMerchantListings(merchantId));
  }

  /**
   * Deactivate a listing by id.
   *
   * @param merchantId current merchant from JWT
   * @param listingId listing id from path
   * @return success response with no data
   */
  @DeleteMapping("/api/merchant/listings/{id}")
  public ApiResponse<Void> deactivateListing(
      @AuthenticationPrincipal Long merchantId,
      @PathVariable("id") Long listingId) {
    productListingService.deactivateListing(merchantId, listingId);
    return ApiResponse.success();
  }
}
