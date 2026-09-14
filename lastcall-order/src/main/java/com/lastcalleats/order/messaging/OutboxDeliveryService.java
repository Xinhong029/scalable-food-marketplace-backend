package com.lastcalleats.order.messaging;

import com.lastcalleats.order.entity.OutboxEventDO;
import com.lastcalleats.order.entity.OutboxEventStatus;
import com.lastcalleats.order.repository.OutboxEventRepo;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns the short database transactions used to claim and finish outbox deliveries. */
@Service
public class OutboxDeliveryService {

  private final OutboxEventRepo outboxEventRepo;
  private final int batchSize;
  private final Duration claimTimeout;
  private final Duration retryBaseDelay;
  private final Duration retryMaxDelay;

  public OutboxDeliveryService(
      OutboxEventRepo outboxEventRepo,
      @Value("${app.messaging.outbox.batch-size:20}") int batchSize,
      @Value("${app.messaging.outbox.claim-timeout:2m}") Duration claimTimeout,
      @Value("${app.messaging.outbox.retry-base-delay:2s}") Duration retryBaseDelay,
      @Value("${app.messaging.outbox.retry-max-delay:1m}") Duration retryMaxDelay) {
    this.outboxEventRepo = outboxEventRepo;
    this.batchSize = batchSize;
    this.claimTimeout = claimTimeout;
    this.retryBaseDelay = retryBaseDelay;
    this.retryMaxDelay = retryMaxDelay;
  }

  /** Atomically claims ready events so concurrent application instances do not share a batch. */
  @Transactional
  public List<OutboxEventDO> claimBatch() {
    LocalDateTime now = LocalDateTime.now();
    outboxEventRepo.releaseStaleClaims(
        OutboxEventStatus.PROCESSING,
        OutboxEventStatus.PENDING,
        now.minus(claimTimeout),
        now);

    List<OutboxEventDO> candidates = outboxEventRepo.findReady(
        OutboxEventStatus.PENDING, now, PageRequest.of(0, batchSize));
    List<OutboxEventDO> claimed = new ArrayList<>();
    for (OutboxEventDO candidate : candidates) {
      if (outboxEventRepo.claim(
          candidate.getId(),
          OutboxEventStatus.PENDING,
          OutboxEventStatus.PROCESSING,
          now) == 1) {
        claimed.add(candidate);
      }
    }
    return claimed;
  }

  /** Marks an event complete only after RabbitMQ has positively acknowledged it. */
  @Transactional
  public boolean markPublished(Long eventId) {
    return outboxEventRepo.markPublished(
        eventId,
        OutboxEventStatus.PROCESSING,
        OutboxEventStatus.PUBLISHED,
        LocalDateTime.now()) == 1;
  }

  /** Returns a failed event to PENDING with bounded exponential backoff. */
  @Transactional
  public boolean reschedule(OutboxEventDO event, Exception failure) {
    long multiplier = 1L << Math.min(event.getRetryCount(), 10);
    long delayMillis = Math.min(
        retryMaxDelay.toMillis(),
        retryBaseDelay.toMillis() * multiplier);
    String error = failure.getMessage() == null
        ? failure.getClass().getSimpleName()
        : failure.getClass().getSimpleName() + ": " + failure.getMessage();
    if (error.length() > 1000) {
      error = error.substring(0, 1000);
    }

    return outboxEventRepo.reschedule(
        event.getId(),
        OutboxEventStatus.PROCESSING,
        OutboxEventStatus.PENDING,
        LocalDateTime.now().plusNanos(delayMillis * 1_000_000),
        error) == 1;
  }
}
