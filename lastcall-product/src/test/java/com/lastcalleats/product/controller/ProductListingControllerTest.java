package com.lastcalleats.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lastcalleats.product.dto.ListingRequest;
import com.lastcalleats.product.dto.ListingResponse;
import com.lastcalleats.product.dto.UserBrowseResponse;
import com.lastcalleats.product.service.ProductListingService;
import com.lastcalleats.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Unit tests for {@link ProductListingController}.
 *
 * <p>This test class verifies that the controller correctly handles HTTP
 * requests, delegates work to the service layer, and returns successful
 * responses.</p>
 *
 * <p>These tests use standalone MockMvc setup, so they do not require a full
 * Spring Boot application context.</p>
 */
@ExtendWith(MockitoExtension.class)
class ProductListingControllerTest {

  private MockMvc mockMvc;

  private ObjectMapper objectMapper;

  @Mock
  private ProductListingService productListingService;

  @InjectMocks
  private ProductListingController productListingController;

  /**
   * Initializes MockMvc and JSON serializer before each test.
   */
  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.findAndRegisterModules();

    mockMvc = MockMvcBuilders
        .standaloneSetup(productListingController)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  /**
   * Verifies that browsing available listings returns HTTP 200
   * and delegates to the service layer.
   */
  @Test
  @DisplayName("browseListings should return 200")
  void browseListings_success_returnsOk() throws Exception {
    when(productListingService.browseAvailableListings(PageRequest.of(0, 20))).thenReturn(
        new SliceImpl<>(List.of(
            UserBrowseResponse.builder()
                .listingId(1L)
                .merchantId(10L)
                .productName("Bread")
                .build()
        ), PageRequest.of(0, 20), true)
    );

    mockMvc.perform(get("/api/products/browse"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].listingId").value(1))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.hasNext").value(true));

    verify(productListingService).browseAvailableListings(PageRequest.of(0, 20));
  }

  @Test
  @DisplayName("browseListings should reject invalid page sizes")
  void browseListings_invalidSize_returnsBadRequest() throws Exception {
    mockMvc.perform(get("/api/products/browse").param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("size must be between 1 and 100"));
  }

  /**
   * Verifies that creating a listing with a valid request returns HTTP 200
   * and delegates to the service layer.
   */
  @Test
  @DisplayName("createListing should return 200 for valid request")
  void createListing_validRequest_returnsOk() throws Exception {
    ListingRequest request = new ListingRequest();
    request.setTemplateId(1L);
    request.setDiscountPrice(new BigDecimal("9.99"));
    request.setQuantity(5);
    request.setPickupStart(LocalTime.of(18, 0));
    request.setPickupEnd(LocalTime.of(20, 0));
    request.setDate(LocalDate.of(2026, 4, 21));

    when(productListingService.createListing(isNull(), any(ListingRequest.class)))
        .thenReturn(
            ListingResponse.builder()
                .id(100L)
                .templateName("Bread")
                .discountPrice(new BigDecimal("9.99"))
                .quantity(5)
                .remainingQuantity(5)
                .build()
        );

    mockMvc.perform(post("/api/merchant/listings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    verify(productListingService).createListing(isNull(), any(ListingRequest.class));
  }

  /**
   * Verifies that fetching merchant listings returns HTTP 200
   * and delegates to the service layer.
   */
  @Test
  @DisplayName("getMerchantListings should return 200")
  void getMerchantListings_success_returnsOk() throws Exception {
    when(productListingService.getMerchantListings(isNull()))
        .thenReturn(
            List.of(
                ListingResponse.builder()
                    .id(1L)
                    .templateName("Bread")
                    .discountPrice(new BigDecimal("9.99"))
                    .quantity(5)
                    .remainingQuantity(3)
                    .build()
            )
        );

    mockMvc.perform(get("/api/merchant/listings"))
        .andExpect(status().isOk());

    verify(productListingService).getMerchantListings(isNull());
  }

  /**
   * Verifies that deactivating a listing returns HTTP 200
   * and delegates to the service layer.
   */
  @Test
  @DisplayName("deactivateListing should return 200")
  void deactivateListing_success_returnsOk() throws Exception {
    doNothing().when(productListingService).deactivateListing(isNull(), eq(1L));

    mockMvc.perform(delete("/api/merchant/listings/1"))
        .andExpect(status().isOk());

    verify(productListingService).deactivateListing(isNull(), eq(1L));
  }
}
