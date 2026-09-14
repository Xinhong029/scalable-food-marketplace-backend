package com.lastcalleats.order.service.impl;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import com.lastcalleats.order.dto.CodeRequest;
import com.lastcalleats.order.dto.CodeResponse;
import com.lastcalleats.order.entity.OrderDO;
import com.lastcalleats.order.entity.PickupCodeDO;
import com.lastcalleats.order.repository.OrderRepo;
import com.lastcalleats.order.repository.PickupCodeRepo;
import com.lastcalleats.order.service.PickupCodeService;
import com.lastcalleats.order.state.PaidState;
import com.lastcalleats.product.entity.ProductListingDO;
import com.lastcalleats.product.entity.ProductTemplateDO;
import com.lastcalleats.product.repository.ProductListingRepo;
import com.lastcalleats.product.repository.ProductTemplateRepo;
import com.lastcalleats.user.entity.UserDO;
import com.lastcalleats.user.repository.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Provides the default implementation of {@link PickupCodeService} for merchant pickup
 * verification. It validates the presented code, updates the order lifecycle, and returns a summary
 * that can be shown at pickup time.
 */
@Service
public class PickupCodeServiceImpl implements PickupCodeService {

  private final PickupCodeRepo pickupCodeRepo;
  private final OrderRepo orderRepo;
  private final UserRepo userRepo;
  private final ProductListingRepo productListingRepo;
  private final ProductTemplateRepo productTemplateRepo;

  /**
   * Creates the service with the repositories needed to validate pickup codes and build the
   * verification response.
   *
   * @param pickupCodeRepo      repository for pickup code records
   * @param orderRepo           repository for order records
   * @param userRepo            repository for user records
   * @param productListingRepo  repository for product listings
   * @param productTemplateRepo repository for product templates
   */
  public PickupCodeServiceImpl(PickupCodeRepo pickupCodeRepo,
      OrderRepo orderRepo,
      UserRepo userRepo,
      ProductListingRepo productListingRepo,
      ProductTemplateRepo productTemplateRepo) {
    this.pickupCodeRepo = pickupCodeRepo;
    this.orderRepo = orderRepo;
    this.userRepo = userRepo;
    this.productListingRepo = productListingRepo;
    this.productTemplateRepo = productTemplateRepo;
  }

  /**
   * Verifies a merchant-submitted pickup credential and completes the order when it is valid.
   *
   * @param merchantId ID of the merchant performing verification
   * @param request    request containing a numeric code or QR payload
   * @return verification result for the pickup attempt
   */
  @Override
  @Transactional
  public CodeResponse verifyPickupCode(Long merchantId, CodeRequest request) {
    // Check whether the input is a numeric code or a QR string.
    PickupCodeDO pickupCode = resolvePickupCode(merchantId, request)
        .orElseThrow(() -> new BusinessException(ErrorCode.PICKUP_CODE_INVALID));

    // Check if the pickup code is already used.
    if (Boolean.TRUE.equals(pickupCode.getUsed())) {
      throw new BusinessException(ErrorCode.PICKUP_CODE_ALREADY_USED);
    }

    // Find the order for this code.
    OrderDO order = orderRepo.findById(pickupCode.getOrderId())
        .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    // The order must be in PAID status.
    if (order.getStatus() != OrderDO.OrderStatus.PAID) {
      throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "Order is not ready for pickup");
    }

    // Change the order status to COMPLETED.
    new PaidState().complete(order);
    // Save the order.
    orderRepo.save(order);
    // Mark the pickup code as used.
    pickupCode.setUsed(true);
    pickupCodeRepo.save(pickupCode);

    // Load user information.
    UserDO user = userRepo.findById(order.getUserId())
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    // Load the product listing.
    ProductListingDO listing = productListingRepo.findById(order.getListingId())
        .orElseThrow(() -> new BusinessException(ErrorCode.LISTING_NOT_FOUND));
    // Load the product template.
    ProductTemplateDO template = productTemplateRepo.findById(listing.getTemplateId())
        .orElseThrow(() -> new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND));

    // Build the response object.
    return CodeResponse.builder()
        .orderId(order.getId())
        .customerNickname(user.getNickname())
        .productName(template.getName())
        .success(true)
        .message("Pickup code verified successfully")
        .build();
  }

  /**
   * Finds a pickup code record from a numeric code or a QR string.
   *
   * @param merchantId current merchant ID
   * @param request    verification request
   * @return matching pickup code if found
   */
  private Optional<PickupCodeDO> resolvePickupCode(Long merchantId, CodeRequest request) {
    String numericCode = request.getPickupCode();
    if (numericCode != null && !numericCode.isBlank()) {
      return pickupCodeRepo.findByMerchantIdAndNumericCode(merchantId, numericCode);
    }

    String qrCodeContent = request.getQrCodeContent();
    if (qrCodeContent != null && !qrCodeContent.isBlank()) {
      return pickupCodeRepo.findByMerchantIdAndQrCode(merchantId, qrCodeContent);
    }

    return Optional.empty();
  }
}
