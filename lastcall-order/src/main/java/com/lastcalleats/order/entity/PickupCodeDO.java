package com.lastcalleats.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Maps the pickup credential record stored in the {@code pickup_code} table. It keeps the generated
 * numeric and QR-based pickup values separate from the main order entity while preserving a
 * one-to-one link back to the order.
 */
@Entity
@Table(name = "pickup_code")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PickupCodeDO {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "order_id", nullable = false, unique = true)
  private Long orderId;

  @Column(name = "numeric_code", nullable = false, length = 6)
  private String numericCode;

  @Column(name = "qr_code", columnDefinition = "TEXT")
  private String qrCode;

  @Column(name = "used", nullable = false)
  private Boolean used = false;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;
}
