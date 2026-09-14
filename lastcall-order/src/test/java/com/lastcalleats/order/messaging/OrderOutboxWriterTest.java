package com.lastcalleats.order.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lastcalleats.order.entity.OrderDO;
import com.lastcalleats.order.entity.OutboxEventDO;
import com.lastcalleats.order.entity.OutboxEventStatus;
import com.lastcalleats.order.repository.OutboxEventRepo;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifies that a saved order becomes a complete durable ORDER_CREATED event. */
@ExtendWith(MockitoExtension.class)
class OrderOutboxWriterTest {

  @Mock
  private OutboxEventRepo outboxEventRepo;

  @Test
  void appendOrderCreated_savesRoutingMetadataAndJsonPayload() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    OrderOutboxWriter writer = new OrderOutboxWriter(outboxEventRepo, objectMapper);
    when(outboxEventRepo.save(any(OutboxEventDO.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    writer.appendOrderCreated(OrderDO.builder()
        .id(101L)
        .userId(201L)
        .merchantId(301L)
        .listingId(401L)
        .price(new BigDecimal("8.99"))
        .build());

    ArgumentCaptor<OutboxEventDO> captor = ArgumentCaptor.forClass(OutboxEventDO.class);
    verify(outboxEventRepo).save(captor.capture());
    OutboxEventDO saved = captor.getValue();
    JsonNode payload = objectMapper.readTree(saved.getPayload());

    assertEquals(OutboxEventStatus.PENDING, saved.getStatus());
    assertEquals(0, saved.getRetryCount());
    assertEquals(OrderMessagingTopology.ORDER_EXCHANGE, saved.getExchangeName());
    assertEquals(OrderMessagingTopology.ORDER_CREATED_ROUTING_KEY, saved.getRoutingKey());
    assertEquals(101L, payload.get("orderId").asLong());
    assertEquals(301L, payload.get("merchantId").asLong());
    assertEquals(OrderCreatedEvent.TYPE, payload.get("eventType").asText());
    assertEquals(saved.getEventId(), payload.get("eventId").asText());
  }
}
