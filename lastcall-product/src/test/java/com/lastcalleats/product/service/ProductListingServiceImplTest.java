package com.lastcalleats.product.service.impl;

import com.lastcalleats.product.cache.ProductBrowseCache;
import com.lastcalleats.product.cache.ProductBrowseCacheEntry;
import com.lastcalleats.product.dto.ListingRequest;
import com.lastcalleats.product.dto.ListingResponse;
import com.lastcalleats.product.dto.UserBrowseProjection;
import com.lastcalleats.product.dto.UserBrowseResponse;
import com.lastcalleats.product.entity.ProductListingDO;
import com.lastcalleats.product.entity.ProductTemplateDO;
import com.lastcalleats.product.metrics.ProductBrowseMetrics;
import com.lastcalleats.product.repository.ProductListingRepo;
import com.lastcalleats.product.repository.ProductTemplateRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ProductListingServiceImpl}.
 *
 * <p>This test class verifies the business logic for creating listings,
 * retrieving a merchant's active listings, deactivating listings,
 * and browsing available listings.</p>
 *
 * <p>All repository dependencies are mocked so these tests focus only
 * on service-layer behavior.</p>
 */
@ExtendWith(MockitoExtension.class)
class ProductListingServiceImplTest {

  @Mock
  private ProductListingRepo productListingRepo;

  @Mock
  private ProductTemplateRepo productTemplateRepo;

  @Mock
  private ProductBrowseCache productBrowseCache;

  @Mock
  private ProductBrowseMetrics productBrowseMetrics;

  @InjectMocks
  private ProductListingServiceImpl productListingService;

  private Long merchantId;
  private ListingRequest listingRequest;
  private ProductTemplateDO ownedTemplate;

  /**
   * Initializes common test data before each test.
   */
  @BeforeEach
  void setUp() {
    merchantId = 1L;

    listingRequest = new ListingRequest();
    listingRequest.setTemplateId(10L);
    listingRequest.setDiscountPrice(new BigDecimal("9.99"));
    listingRequest.setQuantity(5);
    listingRequest.setPickupStart(LocalTime.of(18, 0));
    listingRequest.setPickupEnd(LocalTime.of(20, 0));
    listingRequest.setDate(LocalDate.of(2026, 4, 21));

    ownedTemplate = ProductTemplateDO.builder()
        .id(10L)
        .merchantId(1L)
        .name("Bread")
        .description("Fresh bread")
        .originalPrice(new BigDecimal("15.99"))
        .isActive(true)
        .build();
  }

  /**
   * Verifies that creating a listing with a valid owned template succeeds
   * and initializes stock and availability correctly.
   */
  @Test
  @DisplayName("createListing should create and return a listing response")
  void createListing_validRequest_returnsResponse() {
    ProductListingDO savedListing = ProductListingDO.builder()
        .id(100L)
        .merchantId(merchantId)
        .templateId(10L)
        .discountPrice(new BigDecimal("9.99"))
        .quantity(5)
        .remainingQuantity(5)
        .pickupStart(LocalTime.of(18, 0))
        .pickupEnd(LocalTime.of(20, 0))
        .date(LocalDate.of(2026, 4, 21))
        .isAvailable(true)
        .build();

    when(productTemplateRepo.findById(10L)).thenReturn(Optional.of(ownedTemplate));
    when(productListingRepo.save(any(ProductListingDO.class))).thenReturn(savedListing);

    ListingResponse response = productListingService.createListing(merchantId, listingRequest);

    assertNotNull(response);
    assertEquals(100L, response.getId());
    assertEquals(merchantId, response.getMerchantId());
    assertEquals(10L, response.getTemplateId());
    assertEquals("Bread", response.getTemplateName());
    assertEquals(new BigDecimal("15.99"), response.getOriginalPrice());
    assertEquals(new BigDecimal("9.99"), response.getDiscountPrice());
    assertEquals(5, response.getQuantity());
    assertEquals(5, response.getRemainingQuantity());
    assertTrue(response.getIsAvailable());

    ArgumentCaptor<ProductListingDO> captor = ArgumentCaptor.forClass(ProductListingDO.class);
    verify(productListingRepo).save(captor.capture());
    verify(productBrowseCache).invalidateAllAfterCommit();

    ProductListingDO toSave = captor.getValue();
    assertEquals(merchantId, toSave.getMerchantId());
    assertEquals(10L, toSave.getTemplateId());
    assertEquals(5, toSave.getQuantity());
    assertEquals(5, toSave.getRemainingQuantity());
    assertTrue(toSave.getIsAvailable());
  }

  /**
   * Verifies that creating a listing fails when the template does not exist.
   */
  @Test
  @DisplayName("createListing should throw when template is missing")
  void createListing_templateNotFound_throwsException() {
    when(productTemplateRepo.findById(10L)).thenReturn(Optional.empty());

    RuntimeException ex = assertThrows(RuntimeException.class,
        () -> productListingService.createListing(merchantId, listingRequest));

    assertEquals("Template not found.", ex.getMessage());
    verify(productListingRepo, never()).save(any());
  }

  /**
   * Verifies that creating a listing fails when the template belongs to another merchant.
   */
  @Test
  @DisplayName("createListing should throw when template belongs to another merchant")
  void createListing_templateOwnedByAnotherMerchant_throwsException() {
    ProductTemplateDO anotherMerchantTemplate = ProductTemplateDO.builder()
        .id(10L)
        .merchantId(999L)
        .name("Bread")
        .originalPrice(new BigDecimal("15.99"))
        .build();

    when(productTemplateRepo.findById(10L)).thenReturn(Optional.of(anotherMerchantTemplate));

    RuntimeException ex = assertThrows(RuntimeException.class,
        () -> productListingService.createListing(merchantId, listingRequest));

    assertEquals("You cannot use another merchant's template.", ex.getMessage());
    verify(productListingRepo, never()).save(any());
  }

  /**
   * Verifies that retrieving merchant listings returns only active listings
   * mapped into response DTOs with template information.
   */
  @Test
  @DisplayName("getMerchantListings should return mapped active listings")
  void getMerchantListings_success_returnsMappedResponses() {
    ProductListingDO listing = ProductListingDO.builder()
        .id(101L)
        .merchantId(merchantId)
        .templateId(10L)
        .discountPrice(new BigDecimal("9.99"))
        .quantity(5)
        .remainingQuantity(3)
        .pickupStart(LocalTime.of(18, 0))
        .pickupEnd(LocalTime.of(20, 0))
        .date(LocalDate.of(2026, 4, 21))
        .isAvailable(true)
        .build();

    when(productListingRepo.findByMerchantIdAndIsAvailableTrue(merchantId))
        .thenReturn(List.of(listing));
    when(productTemplateRepo.findById(10L)).thenReturn(Optional.of(ownedTemplate));

    List<ListingResponse> result = productListingService.getMerchantListings(merchantId);

    assertEquals(1, result.size());
    ListingResponse response = result.get(0);
    assertEquals(101L, response.getId());
    assertEquals("Bread", response.getTemplateName());
    assertEquals(new BigDecimal("15.99"), response.getOriginalPrice());
    assertEquals(new BigDecimal("9.99"), response.getDiscountPrice());
    assertEquals(3, response.getRemainingQuantity());
    assertTrue(response.getIsAvailable());
  }

  /**
   * Verifies that deactivating an owned listing marks it unavailable and saves it.
   */
  @Test
  @DisplayName("deactivateListing should mark listing unavailable")
  void deactivateListing_ownedListing_marksUnavailableAndSaves() {
    ProductListingDO listing = ProductListingDO.builder()
        .id(101L)
        .merchantId(merchantId)
        .templateId(10L)
        .isAvailable(true)
        .build();

    when(productListingRepo.findById(101L)).thenReturn(Optional.of(listing));

    productListingService.deactivateListing(merchantId, 101L);

    assertFalse(listing.getIsAvailable());
    verify(productListingRepo).save(listing);
    verify(productBrowseCache).invalidateAllAfterCommit();
  }

  /**
   * Verifies that deactivating a missing listing throws an exception.
   */
  @Test
  @DisplayName("deactivateListing should throw when listing is missing")
  void deactivateListing_listingNotFound_throwsException() {
    when(productListingRepo.findById(101L)).thenReturn(Optional.empty());

    RuntimeException ex = assertThrows(RuntimeException.class,
        () -> productListingService.deactivateListing(merchantId, 101L));

    assertEquals("Listing not found.", ex.getMessage());
    verify(productListingRepo, never()).save(any());
  }

  /**
   * Verifies that deactivating another merchant's listing throws an exception.
   */
  @Test
  @DisplayName("deactivateListing should throw when listing belongs to another merchant")
  void deactivateListing_listingOwnedByAnotherMerchant_throwsException() {
    ProductListingDO listing = ProductListingDO.builder()
        .id(101L)
        .merchantId(999L)
        .templateId(10L)
        .isAvailable(true)
        .build();

    when(productListingRepo.findById(101L)).thenReturn(Optional.of(listing));

    RuntimeException ex = assertThrows(RuntimeException.class,
        () -> productListingService.deactivateListing(merchantId, 101L));

    assertEquals("You cannot operate on another merchant's listing.", ex.getMessage());
    verify(productListingRepo, never()).save(any());
  }

  /** Verifies projection mapping and the extra-row hasNext calculation. */
  @Test
  @DisplayName("browseAvailableListings should return joined browse projections")
  void browseAvailableListings_returnsJoinedProjection() {
    Pageable pageable = PageRequest.of(0, 20);
    UserBrowseProjection projection = mock(UserBrowseProjection.class);
    when(projection.getListingId()).thenReturn(201L);
    when(projection.getTemplateId()).thenReturn(10L);
    when(projection.getMerchantId()).thenReturn(merchantId);
    when(projection.getMerchantName()).thenReturn("Sakura Sushi");
    when(projection.getMerchantAddress()).thenReturn("123 Main St");
    when(projection.getProductName()).thenReturn("Bread");
    when(projection.getDescription()).thenReturn("Fresh bread");
    when(projection.getOriginalPrice()).thenReturn(new BigDecimal("15.99"));
    when(projection.getDiscountPrice()).thenReturn(new BigDecimal("9.99"));
    when(projection.getRemainingQuantity()).thenReturn(3);
    when(projection.getPickupStart()).thenReturn(LocalTime.of(18, 0));
    when(projection.getPickupEnd()).thenReturn(LocalTime.of(20, 0));
    when(productListingRepo.findAvailableForBrowse(21, 0))
        .thenReturn(Collections.nCopies(21, projection));
    when(productBrowseCache.get(0, 20)).thenReturn(Optional.empty());

    Slice<UserBrowseResponse> result = productListingService.browseAvailableListings(pageable);

    assertEquals(20, result.getNumberOfElements());
    assertTrue(result.hasNext());
    UserBrowseResponse response = result.getContent().get(0);
    assertEquals(201L, response.getListingId());
    assertEquals(merchantId, response.getMerchantId());
    assertEquals("Sakura Sushi", response.getMerchantName());
    assertEquals("123 Main St", response.getMerchantAddress());
    assertEquals("Bread", response.getProductName());
    assertEquals("Fresh bread", response.getDescription());
    assertEquals(new BigDecimal("15.99"), response.getOriginalPrice());
    assertEquals(new BigDecimal("9.99"), response.getDiscountPrice());
    assertEquals(3, response.getRemainingQuantity());
    verify(productListingRepo).findAvailableForBrowse(21, 0);
    verify(productBrowseCache).put(any(ProductBrowseCacheEntry.class));
    verify(productBrowseMetrics).recordCacheAccess(ProductBrowseMetrics.CACHE_MISS);
    verify(productBrowseMetrics).recordRequest(
        isNull(), eq(ProductBrowseMetrics.CACHE_MISS),
        eq(ProductBrowseMetrics.OUTCOME_SUCCESS));
    verifyNoInteractions(productTemplateRepo);
  }

  /** Verifies that a cache hit avoids the database browse query. */
  @Test
  @DisplayName("browseAvailableListings should return a cached page without querying MySQL")
  void browseAvailableListings_cacheHit_skipsDatabase() {
    Pageable pageable = PageRequest.of(2, 20);
    UserBrowseResponse cachedProduct = UserBrowseResponse.builder()
        .listingId(301L)
        .productName("Cached Bread")
        .build();
    ProductBrowseCacheEntry cachedPage = new ProductBrowseCacheEntry(
        List.of(cachedProduct), 2, 20, false);
    when(productBrowseCache.get(2, 20)).thenReturn(Optional.of(cachedPage));

    Slice<UserBrowseResponse> result = productListingService.browseAvailableListings(pageable);

    assertEquals(1, result.getNumberOfElements());
    assertEquals("Cached Bread", result.getContent().get(0).getProductName());
    assertFalse(result.hasNext());
    verifyNoInteractions(productListingRepo);
    verify(productBrowseCache, never()).put(any());
    verify(productBrowseMetrics).recordCacheAccess(ProductBrowseMetrics.CACHE_HIT);
    verify(productBrowseMetrics).recordRequest(
        isNull(), eq(ProductBrowseMetrics.CACHE_HIT),
        eq(ProductBrowseMetrics.OUTCOME_SUCCESS));
  }

  /** Verifies that database failures are visible in browse outcome metrics. */
  @Test
  @DisplayName("browseAvailableListings should record a failed cache-miss request")
  void browseAvailableListings_databaseFailure_recordsFailure() {
    Pageable pageable = PageRequest.of(0, 20);
    when(productBrowseCache.get(0, 20)).thenReturn(Optional.empty());
    when(productListingRepo.findAvailableForBrowse(21, 0))
        .thenThrow(new IllegalStateException("database unavailable"));

    assertThrows(IllegalStateException.class,
        () -> productListingService.browseAvailableListings(pageable));

    verify(productBrowseMetrics).recordCacheAccess(ProductBrowseMetrics.CACHE_MISS);
    verify(productBrowseMetrics).recordRequest(
        isNull(), eq(ProductBrowseMetrics.CACHE_MISS),
        eq(ProductBrowseMetrics.OUTCOME_FAILURE));
  }
}
