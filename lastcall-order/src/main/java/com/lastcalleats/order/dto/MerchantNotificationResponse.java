package com.lastcalleats.order.dto;

import java.time.LocalDateTime;

/** Notification returned to the authenticated merchant. */
public record MerchantNotificationResponse(
    Long id,
    Long orderId,
    String type,
    String title,
    String content,
    boolean read,
    LocalDateTime createdAt,
    LocalDateTime readAt
) {
}
