package com.lastcalleats.order.entity;

/** Lifecycle of an event waiting to be delivered from MySQL to RabbitMQ. */
public enum OutboxEventStatus {
  PENDING,
  PROCESSING,
  PUBLISHED
}
