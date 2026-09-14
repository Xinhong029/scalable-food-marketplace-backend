package com.lastcalleats.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/** Notification created asynchronously for a merchant after an order event is consumed. */
@Entity
@Table(name = "merchant_notification")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantNotificationDO {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "merchant_id", nullable = false)
  private Long merchantId;

  @Column(name = "order_id", nullable = false)
  private Long orderId;

  @Column(name = "event_id", nullable = false, unique = true, length = 36)
  private String eventId;

  @Column(name = "notification_type", nullable = false, length = 100)
  private String notificationType;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(nullable = false, length = 1000)
  private String content;

  @Column(name = "is_read", nullable = false)
  private boolean read;

  @Column(name = "read_at")
  private LocalDateTime readAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
