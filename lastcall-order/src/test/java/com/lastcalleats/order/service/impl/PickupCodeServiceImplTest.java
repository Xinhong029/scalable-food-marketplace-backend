package com.lastcalleats.order.service.impl;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import com.lastcalleats.order.dto.CodeRequest;
import com.lastcalleats.order.dto.CodeResponse;
import com.lastcalleats.order.entity.OrderDO;
import com.lastcalleats.order.entity.PickupCodeDO;
import com.lastcalleats.order.repository.OrderRepo;
import com.lastcalleats.order.repository.PickupCodeRepo;
import com.lastcalleats.product.entity.ProductListingDO;
import com.lastcalleats.product.entity.ProductTemplateDO;
import com.lastcalleats.product.repository.ProductListingRepo;
import com.lastcalleats.product.repository.ProductTemplateRepo;
import com.lastcalleats.user.entity.UserDO;
import com.lastcalleats.user.repository.UserRepo;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the merchant pickup verification flow implemented by {@link PickupCodeServiceImpl}.
 * The suite covers successful verification, invalid reuse, invalid order state, and QR-based verification paths.
 */
@ExtendWith(MockitoExtension.class)
class PickupCodeServiceImplTest {

  @Mock
  private PickupCodeRepo pickupCodeRepo;

  @Mock
  private OrderRepo orderRepo;

  @Mock
  private UserRepo userRepo;

  @Mock
  private ProductListingRepo productListingRepo;

  @Mock
  private ProductTemplateRepo productTemplateRepo;

  @InjectMocks
  private PickupCodeServiceImpl pickupCodeService;

  private CodeRequest request;
  private PickupCodeDO pickupCode;
  private OrderDO order;

  /**
   * Initializes the shared request and entity fixtures used across the verification scenarios.
   */
  @BeforeEach
  void setUp() {
    request = new CodeRequest();
    request.setPickupCode("483920");
    request.setQrCodeContent(null);

    pickupCode = PickupCodeDO.builder()
        .id(1L)
        .orderId(10L)
        .numericCode("483920")
        .qrCode("{\"orderId\":10}")
        .used(false)
        .build();

    order = OrderDO.builder()
        .id(10L)
        .userId(100L)
        .listingId(200L)
        .merchantId(300L)
        .price(new BigDecimal("8.99"))
        .status(OrderDO.OrderStatus.PAID)
        .createdAt(LocalDateTime.now())
        .build();
  }

  /**
   * Verifies that a valid numeric pickup code completes the order and marks the code as used.
   */
  @Test
  void verifyPickupCode_shouldCompleteOrderAndConsumeCode() {
    UserDO user = UserDO.builder().id(100L).nickname("Alice").build();
    ProductListingDO listing = ProductListingDO.builder()
        .id(200L)
        .templateId(400L)
        .merchantId(300L)
        .discountPrice(new BigDecimal("8.99"))
        .quantity(5)
        .remainingQuantity(4)
        .pickupStart(LocalTime.of(18, 0))
        .pickupEnd(LocalTime.of(20, 0))
        .date(LocalDate.now())
        .isAvailable(true)
        .build();
    ProductTemplateDO template = ProductTemplateDO.builder()
        .id(400L)
        .merchantId(300L)
        .name("Bakery Bag")
        .originalPrice(new BigDecimal("14.99"))
        .isActive(true)
        .build();

    when(pickupCodeRepo.findByMerchantIdAndNumericCode(300L, "483920")).thenReturn(
        Optional.of(pickupCode));
    when(orderRepo.findById(10L)).thenReturn(Optional.of(order));
    when(userRepo.findById(100L)).thenReturn(Optional.of(user));
    when(productListingRepo.findById(200L)).thenReturn(Optional.of(listing));
    when(productTemplateRepo.findById(400L)).thenReturn(Optional.of(template));

    CodeResponse response = pickupCodeService.verifyPickupCode(300L, request);

    assertEquals(true, response.getSuccess());
    assertEquals("Alice", response.getCustomerNickname());
    assertEquals("Bakery Bag", response.getProductName());
    assertEquals(OrderDO.OrderStatus.COMPLETED, order.getStatus());
    assertEquals(true, pickupCode.getUsed());
    verify(orderRepo).save(any(OrderDO.class));
    verify(pickupCodeRepo).save(any(PickupCodeDO.class));
  }

  /**
   * Verifies that verification fails when the pickup code has already been consumed.
   */
  @Test
  void verifyPickupCode_shouldThrowWhenCodeAlreadyUsed() {
    pickupCode.setUsed(true);
    when(pickupCodeRepo.findByMerchantIdAndNumericCode(300L, "483920")).thenReturn(
        Optional.of(pickupCode));

    BusinessException exception = assertThrows(BusinessException.class,
        () -> pickupCodeService.verifyPickupCode(300L, request));

    assertEquals(ErrorCode.PICKUP_CODE_ALREADY_USED, exception.getErrorCode());
  }

  /**
   * Verifies that verification fails when the order is not yet in the paid state.
   */
  @Test
  void verifyPickupCode_shouldThrowWhenOrderIsNotPaid() {
    order.setStatus(OrderDO.OrderStatus.PENDING_PAYMENT);
    when(pickupCodeRepo.findByMerchantIdAndNumericCode(300L, "483920")).thenReturn(
        Optional.of(pickupCode));
    when(orderRepo.findById(10L)).thenReturn(Optional.of(order));

    BusinessException exception = assertThrows(BusinessException.class,
        () -> pickupCodeService.verifyPickupCode(300L, request));

    assertEquals(ErrorCode.ORDER_STATUS_INVALID, exception.getErrorCode());
  }

  /**
   * Verifies that verification fails when the request does not contain a numeric code or QR payload.
   */
  @Test
  void verifyPickupCode_shouldThrowWhenRequestHasNoCredential() {
    CodeRequest emptyRequest = new CodeRequest();

    BusinessException exception = assertThrows(BusinessException.class,
        () -> pickupCodeService.verifyPickupCode(300L, emptyRequest));

    assertEquals(ErrorCode.PICKUP_CODE_INVALID, exception.getErrorCode());
  }

  /**
   * Verifies that the service also accepts the stored QR payload as a pickup credential.
   */
  @Test
  void verifyPickupCode_shouldSupportQrCodeVerification() {
    CodeRequest qrRequest = new CodeRequest();
    qrRequest.setQrCodeContent("{\"orderId\":10}");

    UserDO user = UserDO.builder().id(100L).nickname("Alice").build();
    ProductListingDO listing = ProductListingDO.builder()
        .id(200L)
        .templateId(400L)
        .merchantId(300L)
        .discountPrice(new BigDecimal("8.99"))
        .quantity(5)
        .remainingQuantity(4)
        .pickupStart(LocalTime.of(18, 0))
        .pickupEnd(LocalTime.of(20, 0))
        .date(LocalDate.now())
        .isAvailable(true)
        .build();
    ProductTemplateDO template = ProductTemplateDO.builder()
        .id(400L)
        .merchantId(300L)
        .name("Bakery Bag")
        .originalPrice(new BigDecimal("14.99"))
        .isActive(true)
        .build();

    when(pickupCodeRepo.findByMerchantIdAndQrCode(300L, "{\"orderId\":10}")).thenReturn(
        Optional.of(pickupCode));
    when(orderRepo.findById(10L)).thenReturn(Optional.of(order));
    when(userRepo.findById(100L)).thenReturn(Optional.of(user));
    when(productListingRepo.findById(200L)).thenReturn(Optional.of(listing));
    when(productTemplateRepo.findById(400L)).thenReturn(Optional.of(template));

    CodeResponse response = pickupCodeService.verifyPickupCode(300L, qrRequest);

    assertEquals(true, response.getSuccess());
    assertEquals(OrderDO.OrderStatus.COMPLETED, order.getStatus());
    assertEquals(true, pickupCode.getUsed());
  }
}
