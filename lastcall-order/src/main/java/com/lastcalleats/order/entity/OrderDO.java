package com.lastcalleats.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Maps the core order record stored in the {@code orders} table. It captures ownership, pricing,
 * and lifecycle state so the order module can persist and transition purchases consistently.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDO {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "listing_id", nullable = false)
  private Long listingId;

  @Column(name = "merchant_id", nullable = false)
  private Long merchantId;

  @Column(nullable = false, precision = 10, scale = 2)
  // Up to 10 digits total, with 2 digits after the decimal point.
  private BigDecimal price;

  @Enumerated(EnumType.STRING)
  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
  @Column(nullable = false, length = 20)
  private OrderStatus status;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt; // Filled with the current time automatically.

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt; // Updated to the current time automatically.

  /**
   * Defines the lifecycle statuses an order can move through. These values are persisted as strings
   * so state transitions stay readable in the database.
   */
  public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    COMPLETED,
    CANCELLED
  }
}
