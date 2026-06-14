package com.tradevault.service;

import com.tradevault.entity.Notification;
import java.util.List;

public interface NotificationService {
    Notification sendNotification(Long userId, String title, String message, String category);
    void broadcastNotification(List<Long> userIds, String title, String message, String category);
    List<Notification> getNotificationsForUser(Long userId);
    List<Notification> getUnreadNotifications(Long userId);
    void markAsRead(Long notificationId);
}
