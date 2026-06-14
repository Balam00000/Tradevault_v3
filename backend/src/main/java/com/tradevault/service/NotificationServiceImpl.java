package com.tradevault.service;

import com.tradevault.entity.Notification;
import com.tradevault.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * NotificationService — centralises all in-app notification creation and retrieval.
 * All modules should use this service to create notifications rather than
 * directly calling NotificationRepository, ensuring a consistent audit trail.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Send an in-app notification to a specific user.
     *
     * @param userId   target user ID
     * @param title    short notification title
     * @param message  full notification body
     * @param category category tag (LC / BankGuarantee / ExportBill / Compliance / Facility)
     */
    @Transactional
    public Notification sendNotification(Long userId, String title, String message, String category) {
        logger.info("Sending notification: userId={}, category='{}', title='{}'", userId, category, title);
        try {
            Notification notification = new Notification(userId, title, message, category);
            Notification saved = notificationRepository.save(notification);
            logger.debug("Notification persisted: notificationId={}, userId={}, category='{}'",
                    saved.getId(), userId, category);
            return saved;
        } catch (Exception e) {
            logger.error("Failed to persist notification for userId={}, category='{}': {}", userId, category, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Broadcast a notification to multiple users (e.g., all ops officers).
     */
    @Transactional
    public void broadcastNotification(List<Long> userIds, String title, String message, String category) {
        logger.info("Broadcasting notification to {} users: category='{}', title='{}'", userIds.size(), category, title);
        int successCount = 0;
        for (Long userId : userIds) {
            try {
                notificationRepository.save(new Notification(userId, title, message, category));
                successCount++;
            } catch (Exception e) {
                logger.error("Failed to send broadcast notification to userId={}: {}", userId, e.getMessage());
            }
        }
        logger.info("Broadcast completed: {}/{} notifications sent for category='{}'", successCount, userIds.size(), category);
    }

    /**
     * Retrieve all notifications for a user (ordered newest first).
     */
    public List<Notification> getNotificationsForUser(Long userId) {
        logger.debug("Fetching all notifications for userId={}", userId);
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        logger.debug("Retrieved {} notifications for userId={}", notifications.size(), userId);
        return notifications;
    }

    /**
     * Retrieve only unread notifications for a user.
     */
    public List<Notification> getUnreadNotifications(Long userId) {
        logger.debug("Fetching unread notifications for userId={}", userId);
        List<Notification> notifications = notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, com.tradevault.entity.enums.NotificationStatus.UNREAD);
        logger.debug("Retrieved {} unread notifications for userId={}", notifications.size(), userId);
        return notifications;
    }

    /**
     * Mark a notification as read.
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        logger.debug("Marking notificationId={} as read", notificationId);
        notificationRepository.findById(notificationId).ifPresentOrElse(
                n -> {
                    n.setIsRead(true);
                    notificationRepository.save(n);
                    logger.debug("NotificationId={} marked as read", notificationId);
                },
                () -> logger.warn("NotificationId={} not found — cannot mark as read", notificationId)
        );
    }
}
