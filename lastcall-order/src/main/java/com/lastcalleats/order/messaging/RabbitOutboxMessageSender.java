package com.lastcalleats.order.messaging;

import com.lastcalleats.order.entity.OutboxEventDO;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Sends the exact JSON stored in an outbox row and waits for a broker confirmation. */
@Component
public class RabbitOutboxMessageSender {

  private final RabbitTemplate rabbitTemplate;
  private final Duration confirmTimeout;

  public RabbitOutboxMessageSender(
      RabbitTemplate rabbitTemplate,
      @Value("${app.messaging.outbox.confirm-timeout:5s}") Duration confirmTimeout) {
    this.rabbitTemplate = rabbitTemplate;
    this.confirmTimeout = confirmTimeout;
  }

  /** Publishes one persistent message or throws when RabbitMQ does not confirm the delivery. */
  public void send(OutboxEventDO event) throws Exception {
    Message message = MessageBuilder
        .withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
        .setContentEncoding(StandardCharsets.UTF_8.name())
        .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
        .setMessageId(event.getEventId())
        .setHeader("eventId", event.getEventId())
        .setHeader("eventType", event.getEventType())
        .setHeader("aggregateType", event.getAggregateType())
        .setHeader("aggregateId", event.getAggregateId())
        .build();

    CorrelationData correlationData = new CorrelationData(event.getEventId());
    rabbitTemplate.send(event.getExchangeName(), event.getRoutingKey(), message, correlationData);

    CorrelationData.Confirm confirm = correlationData.getFuture()
        .get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);
    if (!confirm.isAck()) {
      throw new IllegalStateException("RabbitMQ rejected message: " + confirm.getReason());
    }
    if (correlationData.getReturned() != null) {
      throw new IllegalStateException(
          "RabbitMQ returned unroutable message: "
              + correlationData.getReturned().getReplyText());
    }
  }
}
