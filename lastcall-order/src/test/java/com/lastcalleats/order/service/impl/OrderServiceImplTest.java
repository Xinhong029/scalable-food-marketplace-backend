package com.lastcalleats.order.service.impl;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import com.lastcalleats.order.dto.OrderRequest;
import com.lastcalleats.order.dto.OrderResponse;
import com.lastcalleats.order.entity.OrderDO;
import com.lastcalleats.order.entity.PickupCodeDO;
import com.lastcalleats.order.factory.PickupCodeFactory;
import com.lastcalleats.order.messaging.OrderOutboxWriter;
import com.lastcalleats.order.repository.OrderRepo;
import com.lastcalleats.order.repository.PickupCodeRepo;
import com.lastcalleats.product.entity.ProductListingDO;
import com.lastcalleats.product.entity.ProductTemplateDO;
import com.lastcalleats.product.cache.ProductBrowseCache;
import com.lastcalleats.product.repository.ProductListingRepo;
import com.lastcalleats.product.repository.ProductTemplateRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the customer and merchant order flows implemented by {@link OrderServiceImpl}.
 * The cases cover order creation, duplicate and sold-out validation, pickup access rules, and payment transitions.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

  @Mock
  private OrderRepo orderRepo;

  @Mock
  private PickupCodeRepo pickupCodeRepo;

  @Mock
  private ProductListingRepo productListingRepo;

  @Mock
  private ProductTemplateRepo productTemplateRepo;

  @Mock
  private PickupCodeFactory pickupCodeFactory;

  @Mock
  private ProductBrowseCache productBrowseCache;

  @Mock
  private OrderOutboxWriter orderOutboxWriter;

  @InjectMocks
  private OrderServiceImpl orderService;

  private OrderRequest request;
  private ProductListingDO listing;
  private ProductTemplateDO template;

  /**
   * Initializes the shared request, listing, and template fixtures used by the service tests.
   */
  @BeforeEach
  void setUp() {
    request = new OrderRequest();
    request.setListingId(10L);

    listing = ProductListingDO.builder()
        .id(10L)
        .merchantId(20L)
        .templateId(30L)
        .discountPrice(new BigDecimal("6.99"))
        .quantity(5)
        .remainingQuantity(3)
        .pickupStart(LocalTime.of(18, 0))
        .pickupEnd(LocalTime.of(20, 0))
        .date(LocalDate.now())
        .isAvailable(true)
        .build();

    template = ProductTemplateDO.builder()
        .id(30L)
        .merchantId(20L)
        .name("Sushi Box")
        .originalPrice(new BigDecimal("12.99"))
        .isActive(true)
        .build();
  }

  /**
   * Verifies that creating an order stores a pending order and decrements the listing inventory.
   */
  @Test
  void createOrder_shouldCreatePendingOrderAndDecreaseInventory() {
    OrderDO savedOrder = OrderDO.builder()
        .id(100L)
        .userId(1L)
        .listingId(10L)
        .merchantId(20L)
        .price(new BigDecimal("6.99"))
        .status(OrderDO.OrderStatus.PENDING_PAYMENT)
        .createdAt(LocalDateTime.now())
        .build();

    when(productListingRepo.findById(10L)).thenReturn(Optional.of(listing));
    when(orderRepo.existsByUserIdAndListingIdAndCreatedAtBetween(any(), any(), any(),
        any())).thenReturn(false);
    when(productListingRepo.reserveOneIfAvailable(10L)).thenReturn(1);
    when(orderRepo.save(any(OrderDO.class))).thenReturn(savedOrder);
    when(pickupCodeRepo.findByOrderId(100L)).thenReturn(Optional.empty());
    when(productTemplateRepo.findById(30L)).thenReturn(Optional.of(template));

    OrderResponse response = orderService.createOrder(1L, request);

    verify(productListingRepo).reserveOneIfAvailable(10L);
    verify(orderOutboxWriter).appendOrderCreated(savedOrder);
    verify(productBrowseCache).invalidateAllAfterCommit();
    assertEquals(OrderDO.OrderStatus.PENDING_PAYMENT.name(), response.getStatus());
    assertEquals("Sushi Box", response.getProductName());
    assertNull(response.getPickupCode());
  }

  /**
   * Verifies that order creation fails when the requested listing does not exist.
   */
  @Test
  void createOrder_shouldThrowWhenListingIsNotFound() {
    when(productListingRepo.findById(10L)).thenReturn(Optional.empty());

    BusinessException exception = assertThrows(BusinessException.class,
        () -> orderService.createOrder(1L, request));

    assertEquals(ErrorCode.LISTING_NOT_FOUND, exception.getErrorCode());
  }

  /**
   * Verifies that order creation fails when the listing is marked as unavailable.
   */
  @Test
  void createOrder_shouldThrowWhenListingIsNotAvailable() {
    listing.setIsAvailable(false);
    when(productListingRepo.findById(10L)).thenReturn(Optional.of(listing));

    BusinessException exception = assertThrows(BusinessException.class,
        () -> orderService.createOrder(1L, request));

    assertEquals(ErrorCode.LISTING_NOT_AVAILABLE, exception.getErrorCode());
  }

  /**
   * Verifies that order creation fails when the listing has no remaining inventory.
   */
  @Test
  void createOrder_shouldThrowWhenListingIsSoldOut() {
    listing.setRemainingQuantity(0);
    when(productListingRepo.findById(10L)).thenReturn(Optional.of(listing));

    BusinessException exception = assertThrows(BusinessException.class,
        () -> orderService.createOrder(1L, request));

    assertEquals(ErrorCode.LISTING_SOLD_OUT, exception.getErrorCode());
  }

  /**
   * Verifies that the service rejects a second order for the same listing on the same day.
   */
  @Test
  void createOrder_shouldThrowWhenUserAlreadyOrderedToday() {
    when(productListingRepo.findById(10L)).thenReturn(Optional.of(listing));
    when(orderRepo.existsByUserIdAndListingIdAndCreatedAtBetween(any(), any(), any(),
        any())).thenReturn(true);

    BusinessException exception = assertThrows(BusinessException.class,
        () -> orderService.createOrder(1L, request));

    assertEquals(ErrorCode.ORDER_ALREADY_EXISTS, exception.getErrorCode());
    verify(productListingRepo, never()).reserveOneIfAvailable(10L);
  }

  /**
   * Verifies that a request which loses the atomic inventory race cannot create an order.
   */
  @Test
  void createOrder_shouldThrowWhenAtomicReservationFindsNoStock() {
    when(productListingRepo.findById(10L)).thenReturn(Optional.of(listing));
    when(orderRepo.existsByUserIdAndListingIdAndCreatedAtBetween(any(), any(), any(),
        any())).thenReturn(false);
    when(productListingRepo.reserveOneIfAvailable(10L)).thenReturn(0);

    BusinessException exception = assertThrows(BusinessException.class,
        () -> orderService.createOrder(1L, request));

    assertEquals(ErrorCode.LISTING_SOLD_OUT, exception.getErrorCode());
    verify(orderRepo, never()).save(any(OrderDO.class));
    verify(orderOutboxWriter, never()).appendOrderCreated(any(OrderDO.class));
    verify(productBrowseCache, never()).invalidateAllAfterCommit();
  }

  /**
   * Verifies that pickup details remain unavailable before the order reaches the paid state.
   */
  @Test
  void getPickupCode_shouldThrowWhenOrderIsNotPaid() {
    OrderDO order = OrderDO.builder()
        .id(100L)
        .userId(1L)
        .listingId(10L)
        .merchantId(20L)
        .status(OrderDO.OrderStatus.PENDING_PAYMENT)
        .build();

    when(orderRepo.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(order));
    when(pickupCodeRepo.findByOrderId(100L)).thenReturn(Optional.of(
        PickupCodeDO.builder().orderId(100L).numericCode("123456").build()
    ));

    BusinessException exception = assertThrows(BusinessException.class,
        () -> orderService.getPickupCode(1L, 100L));

    assertEquals(ErrorCode.ORDER_STATUS_INVALID, exception.getErrorCode());
  }

  /**
   * Verifies that pickup details are returned when a paid order already has generated pickup codes.
   */
  @Test
  void getPickupCode_shouldReturnPickupDetailsWhenOrderIsPaid() {
    OrderDO order = OrderDO.builder()
        .id(100L)
        .userId(1L)
        .listingId(10L)
        .merchantId(20L)
        .price(new BigDecimal("6.99"))
        .status(OrderDO.OrderStatus.PAID)
        .createdAt(LocalDateTime.now())
        .build();
    PickupCodeDO pickupCode = PickupCodeDO.builder()
        .orderId(100L)
        .numericCode("123456")
        .qrCode("{\"orderId\":100}")
        .used(false)
        .build();

    when(orderRepo.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(order));
    when(pickupCodeRepo.findByOrderId(100L)).thenReturn(Optional.of(pickupCode));
    when(productListingRepo.findById(10L)).thenReturn(Optional.of(listing));
    when(productTemplateRepo.findById(30L)).thenReturn(Optional.of(template));

    OrderResponse response = orderService.getPickupCode(1L, 100L);

    assertEquals("123456", response.getPickupCode());
    assertEquals("{\"orderId\":100}", response.getQrCodeContent());
    assertEquals(OrderDO.OrderStatus.PAID.name(), response.getStatus());
  }

  /**
   * Verifies that paying an order generates pickup credentials and returns them in the response.
   */
  @Test
  void markOrderPaid_shouldGeneratePickupCodeAndReturnResponse() {
    OrderDO order = OrderDO.builder()
        .id(100L)
        .userId(1L)
        .listingId(10L)
        .merchantId(20L)
        .price(new BigDecimal("6.99"))
        .status(OrderDO.OrderStatus.PENDING_PAYMENT)
        .createdAt(LocalDateTime.now())
        .build();

    PickupCodeDO savedPickupCode = PickupCodeDO.builder()
        .id(200L)
        .orderId(100L)
        .numericCode("654321")
        .qrCode("{\"orderId\":100}")
        .used(false)
        .build();

    when(orderRepo.findById(100L)).thenReturn(Optional.of(order));
    when(orderRepo.save(any(OrderDO.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(pickupCodeFactory.generate(order, "NUMERIC")).thenReturn("654321");
    when(pickupCodeFactory.generate(order, "QR")).thenReturn("{\"orderId\":100}");
    when(pickupCodeRepo.findByOrderId(100L)).thenReturn(Optional.empty(),
        Optional.of(savedPickupCode));
    when(productListingRepo.findById(10L)).thenReturn(Optional.of(listing));
    when(productTemplateRepo.findById(30L)).thenReturn(Optional.of(template));

    OrderResponse response = orderService.markOrderPaid(100L);

    assertEquals(OrderDO.OrderStatus.PAID, order.getStatus());
    assertEquals("654321", response.getPickupCode());
    assertEquals("{\"orderId\":100}", response.getQrCodeContent());
    verify(pickupCodeRepo).save(any(PickupCodeDO.class));
  }

  /**
   * Verifies that an existing pickup code is reused instead of generating a duplicate record.
   */
  @Test
  void markOrderPaid_shouldNotCreateNewPickupCodeWhenOneAlreadyExists() {
    OrderDO order = OrderDO.builder()
        .id(100L)
        .userId(1L)
        .listingId(10L)
        .merchantId(20L)
        .price(new BigDecimal("6.99"))
        .status(OrderDO.OrderStatus.PENDING_PAYMENT)
        .createdAt(LocalDateTime.now())
        .build();

    PickupCodeDO existingPickupCode = PickupCodeDO.builder()
        .id(200L)
        .orderId(100L)
        .numericCode("123456")
        .qrCode("{\"orderId\":100}")
        .used(false)
        .build();

    when(orderRepo.findById(100L)).thenReturn(Optional.of(order));
    when(orderRepo.save(any(OrderDO.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(pickupCodeRepo.findByOrderId(100L)).thenReturn(Optional.of(existingPickupCode),
        Optional.of(existingPickupCode));
    when(productListingRepo.findById(10L)).thenReturn(Optional.of(listing));
    when(productTemplateRepo.findById(30L)).thenReturn(Optional.of(template));

    OrderResponse response = orderService.markOrderPaid(100L);

    assertEquals("123456", response.getPickupCode());
    assertNotNull(response.getQrCodeContent());
    verify(pickupCodeRepo, never()).save(any(PickupCodeDO.class));
  }
}
