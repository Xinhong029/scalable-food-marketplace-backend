package com.lastcalleats.order.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import com.lastcalleats.order.dto.MerchantNotificationResponse;
import com.lastcalleats.order.entity.MerchantNotificationDO;
import com.lastcalleats.order.repository.MerchantNotificationRepo;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifies notification ownership, ordering responses, and read-state changes. */
@ExtendWith(MockitoExtension.class)
class MerchantNotificationServiceImplTest {

  @Mock
  private MerchantNotificationRepo notificationRepo;

  @InjectMocks
  private MerchantNotificationServiceImpl notificationService;

  @Test
  void getNotifications_returnsMerchantNotifications() {
    MerchantNotificationDO notification = notification();
    when(notificationRepo.findByMerchantIdOrderByCreatedAtDesc(20L))
        .thenReturn(List.of(notification));

    List<MerchantNotificationResponse> result = notificationService.getNotifications(20L);

    assertEquals(1, result.size());
    assertEquals(100L, result.getFirst().orderId());
    assertEquals(false, result.getFirst().read());
  }

  @Test
  void markRead_setsReadTimeForOwnedNotification() {
    MerchantNotificationDO notification = notification();
    when(notificationRepo.findByIdAndMerchantId(1L, 20L))
        .thenReturn(Optional.of(notification));

    MerchantNotificationResponse result = notificationService.markRead(20L, 1L);

    assertEquals(true, result.read());
    assertEquals(true, notification.isRead());
    assertEquals(true, notification.getReadAt() != null);
  }

  @Test
  void markRead_doesNotExposeAnotherMerchantsNotification() {
    when(notificationRepo.findByIdAndMerchantId(1L, 20L)).thenReturn(Optional.empty());

    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> notificationService.markRead(20L, 1L));

    assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, exception.getErrorCode());
  }

  private MerchantNotificationDO notification() {
    return MerchantNotificationDO.builder()
        .id(1L)
        .merchantId(20L)
        .orderId(100L)
        .eventId("11111111-1111-1111-1111-111111111111")
        .notificationType("ORDER_CREATED")
        .title("New order #100")
        .content("Order #100 was created")
        .read(false)
        .createdAt(LocalDateTime.now())
        .build();
  }
}
