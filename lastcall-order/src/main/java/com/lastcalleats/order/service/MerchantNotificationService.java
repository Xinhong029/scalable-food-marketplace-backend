package com.lastcalleats.order.service;

import com.lastcalleats.order.dto.MerchantNotificationResponse;
import java.util.List;

/** Read operations for notifications owned by the authenticated merchant. */
public interface MerchantNotificationService {

  List<MerchantNotificationResponse> getNotifications(Long merchantId);

  MerchantNotificationResponse markRead(Long merchantId, Long notificationId);
}
