package com.lastcalleats.order.messaging;

import static com.lastcalleats.order.messaging.OrderMessagingTopology.DEAD_LETTER_EXCHANGE;
import static com.lastcalleats.order.messaging.OrderMessagingTopology.ORDER_CREATED_DEAD_ROUTING_KEY;
import static com.lastcalleats.order.messaging.OrderMessagingTopology.ORDER_CREATED_QUEUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lastcalleats.order.config.OrderMessagingConfig;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;

/** Verifies the order event contract and durable main-queue topology. */
class OrderMessagingConfigTest {

  @Test
  void orderCreatedEvent_roundTripsAsJson() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    OrderCreatedEvent event = OrderCreatedEvent.create(
        101L, 201L, 301L, 401L, new BigDecimal("9.99"));

    String json = objectMapper.writeValueAsString(event);
    OrderCreatedEvent restored = objectMapper.readValue(json, OrderCreatedEvent.class);

    assertEquals(event, restored);
    assertTrue(json.contains("\"eventType\":\"ORDER_CREATED\""));
  }

  @Test
  void orderCreatedQueue_isDurableAndRoutesRejectedMessagesToDlq() {
    Queue queue = new OrderMessagingConfig().orderCreatedQueue();

    assertEquals(ORDER_CREATED_QUEUE, queue.getName());
    assertTrue(queue.isDurable());
    assertEquals(DEAD_LETTER_EXCHANGE, queue.getArguments().get("x-dead-letter-exchange"));
    assertEquals(ORDER_CREATED_DEAD_ROUTING_KEY,
        queue.getArguments().get("x-dead-letter-routing-key"));
  }
}
