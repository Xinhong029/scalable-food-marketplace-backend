package com.lastcalleats.order.service.impl;

import com.lastcalleats.common.exception.BusinessException;
import com.lastcalleats.common.exception.ErrorCode;
import com.lastcalleats.order.dto.MerchantNotificationResponse;
import com.lastcalleats.order.entity.MerchantNotificationDO;
import com.lastcalleats.order.repository.MerchantNotificationRepo;
import com.lastcalleats.order.service.MerchantNotificationService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Default merchant notification query and read-state service. */
@Service
public class MerchantNotificationServiceImpl implements MerchantNotificationService {

  private final MerchantNotificationRepo notificationRepo;

  public MerchantNotificationServiceImpl(MerchantNotificationRepo notificationRepo) {
    this.notificationRepo = notificationRepo;
  }

  @Override
  @Transactional(readOnly = true)
  public List<MerchantNotificationResponse> getNotifications(Long merchantId) {
    return notificationRepo.findByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public MerchantNotificationResponse markRead(Long merchantId, Long notificationId) {
    MerchantNotificationDO notification = notificationRepo
        .findByIdAndMerchantId(notificationId, merchantId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
    if (!notification.isRead()) {
      notification.setRead(true);
      notification.setReadAt(LocalDateTime.now());
    }
    return toResponse(notification);
  }

  private MerchantNotificationResponse toResponse(MerchantNotificationDO notification) {
    return new MerchantNotificationResponse(
        notification.getId(),
        notification.getOrderId(),
        notification.getNotificationType(),
        notification.getTitle(),
        notification.getContent(),
        notification.isRead(),
        notification.getCreatedAt(),
        notification.getReadAt());
  }
}
