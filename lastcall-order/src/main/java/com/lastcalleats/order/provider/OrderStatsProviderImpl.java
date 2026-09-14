package com.lastcalleats.order.provider;

import com.lastcalleats.common.provider.OrderStatsProvider;
import com.lastcalleats.order.entity.OrderDO;
import com.lastcalleats.order.repository.OrderRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Supplies order statistics to other modules through the shared {@link OrderStatsProvider}
 * contract. This implementation keeps merchant dashboard metrics inside the order module while
 * avoiding a direct dependency from common code back to order features.
 */
@Component
@RequiredArgsConstructor
public class OrderStatsProviderImpl implements OrderStatsProvider {

  private final OrderRepo orderRepo;

  /**
   * Counts the orders created today for the specified merchant.
   *
   * @param merchantId ID of the merchant whose orders should be counted
   * @return number of orders created today
   */
  @Override
  public int getTodayOrderCount(Long merchantId) {
    List<OrderDO> orders = orderRepo.findByMerchantId(merchantId);
    LocalDate today = LocalDate.now();
    return (int) orders.stream()
        .filter(order -> order.getCreatedAt().toLocalDate().equals(today))
        .count();
  }

  /**
   * Calculates today's recognized revenue for the specified merchant.
   *
   * @param merchantId ID of the merchant whose revenue should be calculated
   * @return sum of paid or completed order amounts created today
   */
  @Override
  public BigDecimal getTodayRevenue(Long merchantId) {
    List<OrderDO> orders = orderRepo.findByMerchantId(merchantId);
    LocalDate today = LocalDate.now();
    return orders.stream()
        .filter(order -> order.getCreatedAt().toLocalDate().equals(today))
        .filter(order -> order.getStatus() == OrderDO.OrderStatus.PAID
            || order.getStatus() == OrderDO.OrderStatus.COMPLETED)
        .map(OrderDO::getPrice)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
