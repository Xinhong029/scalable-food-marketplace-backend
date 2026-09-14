package com.lastcalleats.order.provider;

import com.lastcalleats.order.entity.OrderDO;
import com.lastcalleats.order.repository.OrderRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Tests the dashboard metrics exposed by {@link OrderStatsProviderImpl}.
 * The cases verify that the provider filters by creation date and only counts revenue from paid or completed orders.
 */
@ExtendWith(MockitoExtension.class)
class OrderStatsProviderImplTest {

  @Mock
  private OrderRepo orderRepo;

  @InjectMocks
  private OrderStatsProviderImpl orderStatsProvider;

  /**
   * Verifies that the provider counts only the orders created today for the requested merchant.
   */
  @Test
  void getTodayOrderCount_shouldCountOnlyOrdersCreatedToday() {
    when(orderRepo.findByMerchantId(20L)).thenReturn(List.of(
        buildOrder(OrderDO.OrderStatus.PENDING_PAYMENT, new BigDecimal("6.99"),
            LocalDateTime.of(LocalDate.now(), java.time.LocalTime.NOON)),
        buildOrder(OrderDO.OrderStatus.PAID, new BigDecimal("8.99"),
            LocalDateTime.of(LocalDate.now(), java.time.LocalTime.MIDNIGHT.plusHours(8))),
        buildOrder(OrderDO.OrderStatus.COMPLETED, new BigDecimal("10.99"),
            LocalDateTime.of(LocalDate.now().minusDays(1), java.time.LocalTime.NOON))
    ));

    int count = orderStatsProvider.getTodayOrderCount(20L);

    assertEquals(2, count);
  }

  /**
   * Verifies that today's revenue includes only paid and completed orders created today.
   */
  @Test
  void getTodayRevenue_shouldIncludeOnlyPaidAndCompletedOrdersCreatedToday() {
    when(orderRepo.findByMerchantId(20L)).thenReturn(List.of(
        buildOrder(OrderDO.OrderStatus.PAID, new BigDecimal("6.99"),
            LocalDateTime.of(LocalDate.now(), java.time.LocalTime.NOON)),
        buildOrder(OrderDO.OrderStatus.COMPLETED, new BigDecimal("8.99"),
            LocalDateTime.of(LocalDate.now(), java.time.LocalTime.NOON.plusHours(1))),
        buildOrder(OrderDO.OrderStatus.PENDING_PAYMENT, new BigDecimal("5.00"),
            LocalDateTime.of(LocalDate.now(), java.time.LocalTime.NOON.plusHours(2))),
        buildOrder(OrderDO.OrderStatus.PAID, new BigDecimal("9.50"),
            LocalDateTime.of(LocalDate.now().minusDays(1), java.time.LocalTime.NOON))
    ));

    BigDecimal revenue = orderStatsProvider.getTodayRevenue(20L);

    assertEquals(new BigDecimal("15.98"), revenue);
  }

  /**
   * Creates an order fixture for the provider test cases.
   *
   * @param status    order status to assign
   * @param price     order price to assign
   * @param createdAt creation timestamp to assign
   * @return order fixture for provider calculations
   */
  private OrderDO buildOrder(OrderDO.OrderStatus status, BigDecimal price, LocalDateTime createdAt) {
    return OrderDO.builder()
        .merchantId(20L)
        .status(status)
        .price(price)
        .createdAt(createdAt)
        .build();
  }
}
