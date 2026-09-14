package com.lastcalleats.order.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lastcalleats.order.entity.OrderDO;
import com.lastcalleats.order.entity.OutboxEventDO;
import com.lastcalleats.order.entity.OutboxEventStatus;
import com.lastcalleats.order.repository.OutboxEventRepo;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Writes order events into MySQL inside the caller's existing order transaction. */
@Component
public class OrderOutboxWriter {

  private static final String ORDER_AGGREGATE = "ORDER";
  private static final Logger LOGGER = LoggerFactory.getLogger(OrderOutboxWriter.class);

  private final OutboxEventRepo outboxEventRepo;
  private final ObjectMapper objectMapper;

  public OrderOutboxWriter(OutboxEventRepo outboxEventRepo, ObjectMapper objectMapper) {
    this.outboxEventRepo = outboxEventRepo;
    this.objectMapper = objectMapper;
  }

  /** Persists an ORDER_CREATED event; calling this without a transaction is an error. */
  @Transactional(propagation = Propagation.MANDATORY)
  public OutboxEventDO appendOrderCreated(OrderDO order) {
    OrderCreatedEvent event = OrderCreatedEvent.create(
        order.getId(), order.getUserId(), order.getMerchantId(), order.getListingId(),
        order.getPrice());

    try {
      OutboxEventDO outboxEvent = outboxEventRepo.save(OutboxEventDO.builder()
          .eventId(event.eventId().toString())
          .aggregateType(ORDER_AGGREGATE)
          .aggregateId(order.getId())
          .eventType(event.eventType())
          .exchangeName(OrderMessagingTopology.ORDER_EXCHANGE)
          .routingKey(OrderMessagingTopology.ORDER_CREATED_ROUTING_KEY)
          .payload(objectMapper.writeValueAsString(event))
          .status(OutboxEventStatus.PENDING)
          .retryCount(0)
          .nextRetryAt(LocalDateTime.now())
          .build());
      LOGGER.info(
          "ORDER_CREATED staged in outbox eventId={} orderId={} merchantId={} status={}",
          event.eventId(), event.orderId(), event.merchantId(), OutboxEventStatus.PENDING);
      return outboxEvent;
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Could not serialize ORDER_CREATED outbox event", exception);
    }
  }
}
