package com.lastcalleats.order.messaging;

import com.lastcalleats.order.entity.MerchantNotificationDO;
import com.lastcalleats.order.repository.MerchantNotificationRepo;
import com.lastcalleats.order.repository.ProcessedEventRepo;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Idempotent consumer that turns ORDER_CREATED messages into merchant notifications. */
@Component
public class MerchantNotificationConsumer {

  public static final String CONSUMER_NAME = "merchant-notification-consumer";
  private static final Logger LOGGER = LoggerFactory.getLogger(MerchantNotificationConsumer.class);

  private final ProcessedEventRepo processedEventRepo;
  private final MerchantNotificationRepo notificationRepo;
  private final OrderMessagingMetrics metrics;

  public MerchantNotificationConsumer(
      ProcessedEventRepo processedEventRepo,
      MerchantNotificationRepo notificationRepo,
      OrderMessagingMetrics metrics) {
    this.processedEventRepo = processedEventRepo;
    this.notificationRepo = notificationRepo;
    this.metrics = metrics;
  }

  /** Applies each event once; returning normally lets the listener container ACK the message. */
  @RabbitListener(queues = OrderMessagingTopology.ORDER_CREATED_QUEUE)
  @Transactional
  public void handleOrderCreated(OrderCreatedEvent event) {
    String eventId = event.eventId().toString();
    LOGGER.info("ORDER_CREATED received eventId={} orderId={} merchantId={}",
        eventId, event.orderId(), event.merchantId());
    try {
      if (processedEventRepo.recordIfAbsent(eventId, CONSUMER_NAME) == 0) {
        metrics.recordConsumerDuplicate();
        LOGGER.info("ORDER_CREATED duplicate skipped eventId={} orderId={} merchantId={}",
            eventId, event.orderId(), event.merchantId());
        return;
      }

      notificationRepo.saveAndFlush(MerchantNotificationDO.builder()
          .merchantId(event.merchantId())
          .orderId(event.orderId())
          .eventId(eventId)
          .notificationType(event.eventType())
          .title("New order #" + event.orderId())
          .content(String.format(
              Locale.ROOT,
              "Order #%d was created for listing #%d. Total: $%s",
              event.orderId(), event.listingId(), event.price().toPlainString()))
          .read(false)
          .build());
      metrics.recordConsumerSuccess(event.occurredAt());
      LOGGER.info("Merchant notification persisted eventId={} orderId={} merchantId={}",
          eventId, event.orderId(), event.merchantId());
    } catch (RuntimeException failure) {
      metrics.recordConsumerFailure();
      LOGGER.warn(
          "ORDER_CREATED processing failed eventId={} orderId={} merchantId={} error={}",
          eventId, event.orderId(), event.merchantId(), failure.getMessage());
      throw failure;
    }
  }
}
