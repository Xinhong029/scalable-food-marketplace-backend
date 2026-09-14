package com.lastcalleats.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/** Idempotency ledger recording which consumer has already handled each event. */
@Entity
@Table(
    name = "processed_event",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_processed_event_consumer",
        columnNames = {"event_id", "consumer_name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEventDO {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "event_id", nullable = false, length = 36)
  private String eventId;

  @Column(name = "consumer_name", nullable = false, length = 100)
  private String consumerName;

  @CreationTimestamp
  @Column(name = "processed_at", nullable = false, updatable = false)
  private LocalDateTime processedAt;
}
