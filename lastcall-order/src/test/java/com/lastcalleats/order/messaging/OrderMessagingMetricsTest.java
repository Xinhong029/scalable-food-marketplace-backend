package com.lastcalleats.order.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.lastcalleats.order.entity.OutboxEventStatus;
import com.lastcalleats.order.repository.OutboxEventRepo;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Verifies metric names, outcome tags, and live outbox status gauges. */
class OrderMessagingMetricsTest {

  @Test
  void recordsPipelineOutcomesAndReadsOutboxGauges() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    OutboxEventRepo outboxEventRepo = Mockito.mock(OutboxEventRepo.class);
    when(outboxEventRepo.countByStatus(OutboxEventStatus.PENDING)).thenReturn(2L);

    OrderMessagingMetrics metrics = new OrderMessagingMetrics(registry, outboxEventRepo);
    metrics.recordPublishSuccess();
    metrics.recordPublishFailure();
    metrics.recordConsumerSuccess(Instant.now().minusMillis(25));
    metrics.recordConsumerDuplicate();
    metrics.recordConsumerFailure();

    assertEquals(1.0, registry.get("lastcall.outbox.publish")
        .tag("outcome", "success").counter().count());
    assertEquals(1.0, registry.get("lastcall.outbox.publish")
        .tag("outcome", "failure").counter().count());
    assertEquals(1.0, registry.get("lastcall.order.created.consumer")
        .tag("outcome", "success").counter().count());
    assertEquals(1.0, registry.get("lastcall.order.created.consumer")
        .tag("outcome", "duplicate").counter().count());
    assertEquals(1.0, registry.get("lastcall.order.created.consumer")
        .tag("outcome", "failure").counter().count());
    assertEquals(1L, registry.get("lastcall.order.created.end.to.end").timer().count());
    assertEquals(2.0, registry.get("lastcall.outbox.events")
        .tag("status", "pending").gauge().value());
  }
}
