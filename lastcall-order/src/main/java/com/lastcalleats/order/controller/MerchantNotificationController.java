package com.lastcalleats.order.controller;

import com.lastcalleats.common.response.ApiResponse;
import com.lastcalleats.order.dto.MerchantNotificationResponse;
import com.lastcalleats.order.service.MerchantNotificationService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Merchant-only HTTP endpoints for asynchronously generated order notifications. */
@RestController
@RequestMapping("/api/merchant/notifications")
public class MerchantNotificationController {

  private final MerchantNotificationService notificationService;

  public MerchantNotificationController(MerchantNotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping
  public ApiResponse<List<MerchantNotificationResponse>> getNotifications(
      @AuthenticationPrincipal Long merchantId) {
    return ApiResponse.success(notificationService.getNotifications(merchantId));
  }

  @PatchMapping("/{id}/read")
  public ApiResponse<MerchantNotificationResponse> markRead(
      @AuthenticationPrincipal Long merchantId,
      @PathVariable Long id) {
    return ApiResponse.success(notificationService.markRead(merchantId, id));
  }
}
