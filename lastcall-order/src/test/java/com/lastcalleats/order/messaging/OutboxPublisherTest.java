package com.lastcalleats.order.messaging;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lastcalleats.order.entity.OutboxEventDO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifies that confirmations finish events and delivery failures schedule retries. */
@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

  @Mock
  private OutboxDeliveryService deliveryService;

  @Mock
  private RabbitOutboxMessageSender messageSender;

  @Mock
  private OrderMessagingMetrics metrics;

  @InjectMocks
  private OutboxPublisher publisher;

  @Test
  void publishPendingEvents_marksConfirmedMessagePublished() throws Exception {
    OutboxEventDO event = event(1L, "event-1");
    when(deliveryService.claimBatch()).thenReturn(List.of(event));
    when(deliveryService.markPublished(1L)).thenReturn(true);

    publisher.publishPendingEvents();

    verify(messageSender).send(event);
    verify(deliveryService).markPublished(1L);
    verify(metrics).recordPublishSuccess();
  }

  @Test
  void publishPendingEvents_reschedulesFailedMessage() throws Exception {
    OutboxEventDO event = event(2L, "event-2");
    IllegalStateException failure = new IllegalStateException("broker unavailable");
    when(deliveryService.claimBatch()).thenReturn(List.of(event));
    doThrow(failure).when(messageSender).send(event);

    publisher.publishPendingEvents();

    verify(deliveryService).reschedule(event, failure);
    verify(metrics).recordPublishFailure();
  }

  private OutboxEventDO event(Long id, String eventId) {
    return OutboxEventDO.builder().id(id).eventId(eventId).build();
  }
}
