package com.lastcalleats.order.messaging;

import com.lastcalleats.order.entity.OutboxEventDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically delivers claimed MySQL outbox events to RabbitMQ. */
@Component
@ConditionalOnProperty(
    prefix = "app.messaging.outbox",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class OutboxPublisher {

  private static final Logger LOGGER = LoggerFactory.getLogger(OutboxPublisher.class);

  private final OutboxDeliveryService deliveryService;
  private final RabbitOutboxMessageSender messageSender;
  private final OrderMessagingMetrics metrics;

  public OutboxPublisher(
      OutboxDeliveryService deliveryService,
      RabbitOutboxMessageSender messageSender,
      OrderMessagingMetrics metrics) {
    this.deliveryService = deliveryService;
    this.messageSender = messageSender;
    this.metrics = metrics;
  }

  /** Publishes a bounded batch and leaves failures in MySQL for a later retry. */
  @Scheduled(fixedDelayString = "${app.messaging.outbox.publish-interval-ms:1000}")
  public void publishPendingEvents() {
    for (OutboxEventDO event : deliveryService.claimBatch()) {
      try {
        messageSender.send(event);
        if (deliveryService.markPublished(event.getId())) {
          metrics.recordPublishSuccess();
          LOGGER.info(
              "ORDER_CREATED published eventId={} orderId={} exchange={} routingKey={}",
              event.getEventId(), event.getAggregateId(), event.getExchangeName(),
              event.getRoutingKey());
        } else {
          metrics.recordPublishFailure();
          LOGGER.warn(
              "Outbox confirmation state conflict eventId={} orderId={} expectedStatus=PROCESSING",
              event.getEventId(), event.getAggregateId());
        }
      } catch (Exception failure) {
        deliveryService.reschedule(event, failure);
        metrics.recordPublishFailure();
        LOGGER.warn(
            "Outbox delivery failed eventId={} orderId={} retryCount={} error={}",
            event.getEventId(), event.getAggregateId(), event.getRetryCount() + 1,
            failure.getMessage());
      }
    }
  }
}
