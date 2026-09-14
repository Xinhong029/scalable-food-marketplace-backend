package com.lastcalleats.order.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lastcalleats.order.entity.MerchantNotificationDO;
import com.lastcalleats.order.repository.MerchantNotificationRepo;
import com.lastcalleats.order.repository.ProcessedEventRepo;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifies first-delivery processing and duplicate-event suppression. */
@ExtendWith(MockitoExtension.class)
class MerchantNotificationConsumerTest {

  @Mock
  private ProcessedEventRepo processedEventRepo;

  @Mock
  private MerchantNotificationRepo notificationRepo;

  @Mock
  private OrderMessagingMetrics metrics;

  @Test
  void handleOrderCreated_firstDeliveryCreatesNotification() {
    MerchantNotificationConsumer consumer =
        new MerchantNotificationConsumer(processedEventRepo, notificationRepo, metrics);
    OrderCreatedEvent event = event();
    when(processedEventRepo.recordIfAbsent(
        event.eventId().toString(), MerchantNotificationConsumer.CONSUMER_NAME)).thenReturn(1);

    consumer.handleOrderCreated(event);

    ArgumentCaptor<MerchantNotificationDO> captor =
        ArgumentCaptor.forClass(MerchantNotificationDO.class);
    verify(notificationRepo).saveAndFlush(captor.capture());
    verify(metrics).recordConsumerSuccess(event.occurredAt());
    MerchantNotificationDO notification = captor.getValue();
    assertEquals(event.eventId().toString(), notification.getEventId());
    assertEquals(301L, notification.getMerchantId());
    assertEquals(101L, notification.getOrderId());
    assertEquals("New order #101", notification.getTitle());
    assertEquals(false, notification.isRead());
  }

  @Test
  void handleOrderCreated_duplicateEventDoesNotCreateAnotherNotification() {
    MerchantNotificationConsumer consumer =
        new MerchantNotificationConsumer(processedEventRepo, notificationRepo, metrics);
    OrderCreatedEvent event = event();
    when(processedEventRepo.recordIfAbsent(
        event.eventId().toString(), MerchantNotificationConsumer.CONSUMER_NAME)).thenReturn(0);

    consumer.handleOrderCreated(event);

    verify(notificationRepo, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    verify(metrics).recordConsumerDuplicate();
  }

  private OrderCreatedEvent event() {
    return new OrderCreatedEvent(
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        OrderCreatedEvent.TYPE,
        101L,
        201L,
        301L,
        401L,
        new BigDecimal("8.99"),
        Instant.parse("2026-09-14T00:00:00Z"));
  }
}
