package com.lastcalleats.order.service;

import com.lastcalleats.order.dto.OrderRequest;
import com.lastcalleats.order.dto.OrderResponse;

import java.util.List;

/**
 * Defines the business operations supported by the order module. These methods cover both
 * customer-facing order retrieval and merchant-facing state transitions.
 */
public interface OrderService {

  /**
   * Creates a new order.
   *
   * @param userId  current user ID
   * @param request order request, with listingId as the main field
   * @return created order
   */
  OrderResponse createOrder(Long userId, OrderRequest request);

  /**
   * Gets all orders for a user.
   *
   * @param userId current user ID
   * @return order list
   */
  List<OrderResponse> getUserOrders(Long userId);

  /**
   * Gets one order for a user.
   *
   * @param userId  current user ID
   * @param orderId order ID
   * @return order detail
   */
  OrderResponse getUserOrderDetail(Long userId, Long orderId);

  /**
   * Gets pickup info for a user order.
   *
   * @param userId  current user ID
   * @param orderId order ID
   * @return order with pickup info
   */
  OrderResponse getPickupCode(Long userId, Long orderId);

  /**
   * Gets all orders for a merchant.
   *
   * @param merchantId current merchant ID
   * @return merchant order list
   */
  List<OrderResponse> getMerchantOrders(Long merchantId);

  /**
   * Marks an order as paid and creates pickup codes.
   *
   * @param orderId order ID
   * @return updated order
   */
  OrderResponse markOrderPaid(Long orderId);
}
