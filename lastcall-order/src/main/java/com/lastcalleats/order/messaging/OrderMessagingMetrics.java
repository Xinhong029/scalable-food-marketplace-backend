package com.lastcalleats.order.messaging;

import com.lastcalleats.order.entity.OutboxEventStatus;
import com.lastcalleats.order.repository.OutboxEventRepo;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Metrics for the MySQL outbox and ORDER_CREATED consumer pipeline. */
@Component
public class OrderMessagingMetrics {

  private final Map<PublishOutcome, Counter> publishCounters =
      new EnumMap<>(PublishOutcome.class);
  private final Map<ConsumerOutcome, Counter> consumerCounters =
      new EnumMap<>(ConsumerOutcome.class);
  private final Timer endToEndLatency;

  public OrderMessagingMetrics(MeterRegistry registry, OutboxEventRepo outboxEventRepo) {
    for (OutboxEventStatus status : OutboxEventStatus.values()) {
      Gauge.builder(
              "lastcall.outbox.events",
              outboxEventRepo,
              repository -> repository.countByStatus(status))
          .description("Current number of outbox rows by delivery status")
          .tag("status", status.name().toLowerCase())
          .register(registry);
    }

    for (PublishOutcome outcome : PublishOutcome.values()) {
      publishCounters.put(outcome, Counter.builder("lastcall.outbox.publish")
          .description("Outbox publication results")
          .tag("outcome", outcome.name().toLowerCase())
          .register(registry));
    }

    for (ConsumerOutcome outcome : ConsumerOutcome.values()) {
      consumerCounters.put(outcome, Counter.builder("lastcall.order.created.consumer")
          .description("ORDER_CREATED consumer attempt results")
          .tag("outcome", outcome.name().toLowerCase())
          .register(registry));
    }

    endToEndLatency = Timer.builder("lastcall.order.created.end.to.end")
        .description("Time from ORDER_CREATED occurrence to notification persistence")
        .publishPercentileHistogram()
        .register(registry);
  }

  public void recordPublishSuccess() {
    publishCounters.get(PublishOutcome.SUCCESS).increment();
  }

  public void recordPublishFailure() {
    publishCounters.get(PublishOutcome.FAILURE).increment();
  }

  public void recordConsumerSuccess(Instant occurredAt) {
    consumerCounters.get(ConsumerOutcome.SUCCESS).increment();
    Duration latency = Duration.between(occurredAt, Instant.now());
    endToEndLatency.record(latency.isNegative() ? Duration.ZERO : latency);
  }

  public void recordConsumerDuplicate() {
    consumerCounters.get(ConsumerOutcome.DUPLICATE).increment();
  }

  public void recordConsumerFailure() {
    consumerCounters.get(ConsumerOutcome.FAILURE).increment();
  }

  private enum PublishOutcome {
    SUCCESS,
    FAILURE
  }

  private enum ConsumerOutcome {
    SUCCESS,
    DUPLICATE,
    FAILURE
  }
}
