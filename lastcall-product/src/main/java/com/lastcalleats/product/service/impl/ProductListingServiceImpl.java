package com.lastcalleats.product.service.impl;

import com.lastcalleats.product.cache.ProductBrowseCache;
import com.lastcalleats.product.cache.ProductBrowseCacheEntry;
import com.lastcalleats.product.dto.ListingRequest;
import com.lastcalleats.product.dto.ListingResponse;
import com.lastcalleats.product.dto.UserBrowseResponse;
import com.lastcalleats.product.entity.ProductListingDO;
import com.lastcalleats.product.entity.ProductTemplateDO;
import com.lastcalleats.product.metrics.ProductBrowseMetrics;
import com.lastcalleats.product.repository.ProductListingRepo;
import com.lastcalleats.product.repository.ProductTemplateRepo;
import com.lastcalleats.product.service.ProductListingService;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for product listing related business logic.
 *
 * <p>A listing is the actual daily item a merchant publishes for sale.
 * This class handles creating listings, retrieving a merchant's listings,
 * deactivating listings, and browsing available listings for users.</p>
 */
@Service
@Transactional
public class ProductListingServiceImpl implements ProductListingService {

  /**
   * Repository for product listing database operations.
   */
  private final ProductListingRepo productListingRepo;

  /**
   * Repository for product template database operations.
   * A listing must be created from an existing template.
   */
  private final ProductTemplateRepo productTemplateRepo;

  /** Redis Cache-Aside access for user-facing browse pages. */
  private final ProductBrowseCache productBrowseCache;

  /** Low-cardinality latency and cache metrics for the browse path. */
  private final ProductBrowseMetrics productBrowseMetrics;

  /**
   * Constructs the listing service with required repositories.
   *
   * @param productListingRepo repository for listing data
   * @param productTemplateRepo repository for template data
   */
  public ProductListingServiceImpl(ProductListingRepo productListingRepo,
      ProductTemplateRepo productTemplateRepo,
      ProductBrowseCache productBrowseCache,
      ProductBrowseMetrics productBrowseMetrics) {
    this.productListingRepo = productListingRepo;
    this.productTemplateRepo = productTemplateRepo;
    this.productBrowseCache = productBrowseCache;
    this.productBrowseMetrics = productBrowseMetrics;
  }

  /**
   * Create a new listing for the current merchant.
   *
   * <p>This method first verifies that the template exists and belongs to
   * the current merchant. It then creates a listing where
   * {@code remainingQuantity} starts equal to {@code quantity}, and
   * {@code isAvailable} is set to true by default.</p>
   *
   * @param merchantId current merchant id
   * @param request listing request data
   * @return the created listing response
   */
  @Override
  public ListingResponse createListing(Long merchantId, ListingRequest request) {
    // Ensure the template exists and belongs to this merchant
    ProductTemplateDO template = getOwnedTemplateOrThrow(merchantId, request.getTemplateId());

    // Build a new listing entity from request data
    ProductListingDO listing = ProductListingDO.builder()
        .merchantId(merchantId)
        .templateId(request.getTemplateId())
        .discountPrice(request.getDiscountPrice())
        .quantity(request.getQuantity())
        .remainingQuantity(request.getQuantity()) // stock starts full
        .pickupStart(request.getPickupStart())
        .pickupEnd(request.getPickupEnd())
        .date(request.getDate())
        .isAvailable(true)
        .build();

    // Save the new listing and convert it to response format
    ProductListingDO savedListing = productListingRepo.save(listing);
    productBrowseCache.invalidateAllAfterCommit();
    return toResponse(savedListing, template);
  }

  /**
   * Get all listings belonging to the current merchant.
   *
   * @param merchantId current merchant id
   * @return a list of the merchant's listing responses
   */
  @Override
  @Transactional(readOnly = true)
  public List<ListingResponse> getMerchantListings(Long merchantId) {
    // Load only active listings for this merchant
    List<ProductListingDO> listings = productListingRepo.findByMerchantIdAndIsAvailableTrue(merchantId);

    // Convert each listing entity to response DTO
    return listings.stream()
        .map(this::toResponseWithTemplateLookup)
        .collect(Collectors.toList());
  }

  /**
   * Soft deactivate a listing.
   *
   * <p>This method does not delete the row from the database.
   * It only marks the listing as unavailable.</p>
   *
   * @param merchantId current merchant id
   * @param listingId listing id to deactivate
   */
  @Override
  public void deactivateListing(Long merchantId, Long listingId) {
    // Ensure the listing exists and belongs to this merchant
    ProductListingDO listing = getOwnedListingOrThrow(merchantId, listingId);

    // Soft deactivate the listing
    listing.setIsAvailable(false);

    // Save the updated listing
    productListingRepo.save(listing);
    productBrowseCache.invalidateAllAfterCommit();
  }

  /**
   * Browse all currently available listings for users.
   *
   * <p>This method is used by the user-facing browse page. The database filters unavailable and
   * sold-out listings, joins the required template and merchant fields, and directly returns one
   * stable slice of UserBrowseResponse DTOs.</p>
   *
   * @param pageable requested zero-based page and page size
   * @return one slice of available listing responses with merchant info
   */
  @Override
  @Transactional(readOnly = true)
  public Slice<UserBrowseResponse> browseAvailableListings(Pageable pageable) {
    Timer.Sample sample = productBrowseMetrics.start();
    String cacheResult = ProductBrowseMetrics.CACHE_UNKNOWN;
    String outcome = ProductBrowseMetrics.OUTCOME_SUCCESS;
    try {
      int pageSize = pageable.getPageSize();
      int pageNumber = pageable.getPageNumber();
      var cachedPage = productBrowseCache.get(pageNumber, pageSize);
      if (cachedPage.isPresent()) {
        cacheResult = ProductBrowseMetrics.CACHE_HIT;
        productBrowseMetrics.recordCacheAccess(cacheResult);
        return cachedPage.get().toSlice(pageable);
      }

      cacheResult = ProductBrowseMetrics.CACHE_MISS;
      productBrowseMetrics.recordCacheAccess(cacheResult);
      List<UserBrowseResponse> rows = productListingRepo
          .findAvailableForBrowse(pageSize + 1, pageable.getOffset())
          .stream()
          .map(UserBrowseResponse::from)
          .toList();
      boolean hasNext = rows.size() > pageSize;
      List<UserBrowseResponse> content = hasNext ? rows.subList(0, pageSize) : rows;
      Slice<UserBrowseResponse> result = new SliceImpl<>(content, pageable, hasNext);
      productBrowseCache.put(new ProductBrowseCacheEntry(content, pageNumber, pageSize, hasNext));
      return result;
    } catch (RuntimeException exception) {
      outcome = ProductBrowseMetrics.OUTCOME_FAILURE;
      throw exception;
    } finally {
      productBrowseMetrics.recordRequest(sample, cacheResult, outcome);
    }
  }

  /**
   * Find a template by id and verify it belongs to the current merchant.
   *
   * @param merchantId current merchant id
   * @param templateId template id
   * @return the owned template entity
   * @throws RuntimeException if the template does not exist or belongs to another merchant
   */
  private ProductTemplateDO getOwnedTemplateOrThrow(Long merchantId, Long templateId) {
    ProductTemplateDO template = productTemplateRepo.findById(templateId)
        .orElseThrow(() -> new RuntimeException("Template not found."));

    // Prevent a merchant from using another merchant's template
    if (!template.getMerchantId().equals(merchantId)) {
      throw new RuntimeException("You cannot use another merchant's template.");
    }

    return template;
  }

  /**
   * Find a listing by id and verify it belongs to the current merchant.
   *
   * @param merchantId current merchant id
   * @param listingId listing id
   * @return the owned listing entity
   * @throws RuntimeException if the listing does not exist or belongs to another merchant
   */
  private ProductListingDO getOwnedListingOrThrow(Long merchantId, Long listingId) {
    ProductListingDO listing = productListingRepo.findById(listingId)
        .orElseThrow(() -> new RuntimeException("Listing not found."));

    // Prevent a merchant from operating on another merchant's listing
    if (!listing.getMerchantId().equals(merchantId)) {
      throw new RuntimeException("You cannot operate on another merchant's listing.");
    }

    return listing;
  }

  /**
   * Convert a listing entity and its template entity into a listing response DTO.
   *
   * @param listing listing entity
   * @param template related template entity
   * @return listing response DTO
   */
  private ListingResponse toResponse(ProductListingDO listing, ProductTemplateDO template) {
    return ListingResponse.builder()
        .id(listing.getId())
        .merchantId(listing.getMerchantId())
        .templateId(listing.getTemplateId())
        .templateName(template.getName())
        .originalPrice(template.getOriginalPrice())
        .discountPrice(listing.getDiscountPrice())
        .quantity(listing.getQuantity())
        .remainingQuantity(listing.getRemainingQuantity())
        .pickupStart(listing.getPickupStart())
        .pickupEnd(listing.getPickupEnd())
        .date(listing.getDate())
        .isAvailable(listing.getIsAvailable())
        .build();
  }

  /**
   * Convert a listing entity into a response DTO by first loading its template.
   *
   * @param listing listing entity
   * @return listing response DTO
   */
  private ListingResponse toResponseWithTemplateLookup(ProductListingDO listing) {
    // Load the related template so response can include template info
    ProductTemplateDO template = productTemplateRepo.findById(listing.getTemplateId())
        .orElseThrow(() -> new RuntimeException("Template not found for listing."));

    return toResponse(listing, template);
  }

}
