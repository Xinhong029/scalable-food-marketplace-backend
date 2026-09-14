package com.lastcalleats.order.controller;

import com.lastcalleats.common.response.ApiResponse;
import com.lastcalleats.order.dto.OrderRequest;
import com.lastcalleats.order.dto.OrderResponse;
import com.lastcalleats.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes HTTP endpoints for the order workflows used by customers and merchants. It translates
 * authenticated API requests into {@link OrderService} calls and wraps the results in a common
 * response format.
 */
@RestController
@RequestMapping("/api")
public class OrderController {

  private final OrderService orderService;

  /**
   * Creates a controller with the order service used by the API layer.
   *
   * @param orderService service that handles order-related business logic
   */
  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  /**
   * Creates a new order.
   *
   * @param userId       current user
   * @param orderRequest order creation payload
   * @return created order
   */
  @PostMapping("/orders")
  public ApiResponse<OrderResponse> createOrder(@AuthenticationPrincipal Long userId,
      @Valid @RequestBody OrderRequest orderRequest) {
    return ApiResponse.success(orderService.createOrder(userId, orderRequest));
  }

  /**
   * Gets all orders for the current user.
   *
   * @param userId current user
   * @return order list
   */
  @GetMapping("/orders")
  public ApiResponse<List<OrderResponse>> getUserOrders(@AuthenticationPrincipal Long userId) {
    return ApiResponse.success(orderService.getUserOrders(userId));
  }

  /**
   * Gets one order for the current user.
   *
   * @param userId current user
   * @param id     order ID
   * @return order detail
   */
  @GetMapping("/orders/{id}")
  public ApiResponse<OrderResponse> getUserOrderDetail(@AuthenticationPrincipal Long userId,
      @PathVariable Long id) {
    return ApiResponse.success(orderService.getUserOrderDetail(userId, id));
  }

  /**
   * Gets pickup info for a paid order.
   *
   * @param userId current user
   * @param id     order ID
   * @return order with pickup info
   */
  @GetMapping("/orders/{id}/pickup-code")
  // Added in V1: this endpoint also returns qrCodeContent.
  public ApiResponse<OrderResponse> getPickupCode(@AuthenticationPrincipal Long userId,
      @PathVariable Long id) {
    return ApiResponse.success(orderService.getPickupCode(userId, id));
  }

  /**
   * Gets all orders for the current merchant.
   *
   * @param merchantId current merchant
   * @return merchant order list
   */
  @GetMapping("/merchant/orders")
  public ApiResponse<List<OrderResponse>> getMerchantOrders(
      @AuthenticationPrincipal Long merchantId) {
    return ApiResponse.success(orderService.getMerchantOrders(merchantId));
  }
}
