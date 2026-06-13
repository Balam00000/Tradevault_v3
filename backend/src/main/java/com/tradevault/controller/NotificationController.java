package com.tradevault.controller;

import com.tradevault.dto.ApiResponse;
import com.tradevault.entity.Notification;
import com.tradevault.entity.User;
import com.tradevault.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private com.tradevault.repository.UserRepository userRepository;

    private void verifyUserAccess(Long userId, Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        if (!user.getId().equals(userId)) {
            logger.warn("Notification access denied: username='{}' (userId={}) attempted to access notifications of userId={}",
                    user.getUsername(), user.getId(), userId);
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to access these notifications");
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<Notification>>> getNotifications(@PathVariable Long userId, Principal principal) {
        logger.debug("GetNotifications for userId={} requested by username='{}'", userId, principal.getName());
        verifyUserAccess(userId, principal);
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        logger.info("Retrieved {} notifications for userId={}", notifications.size(), userId);
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched", notifications));
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<ApiResponse<List<Notification>>> getUnreadNotifications(@PathVariable Long userId, Principal principal) {
        logger.debug("GetUnreadNotifications for userId={} requested by username='{}'", userId, principal.getName());
        verifyUserAccess(userId, principal);
        List<Notification> notifications = notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, com.tradevault.entity.enums.NotificationStatus.UNREAD);
        logger.info("Retrieved {} unread notifications for userId={}", notifications.size(), userId);
        return ResponseEntity.ok(ApiResponse.success("Unread notifications fetched", notifications));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id, Principal principal) {
        logger.debug("MarkAsRead for notificationId={} requested by username='{}'", id, principal.getName());
        Notification notification = notificationRepository.findById(id).orElseThrow();
        User user = userRepository.findByUsername(principal.getName()).orElseThrow();
        if (!notification.getUserId().equals(user.getId())) {
            logger.warn("Mark-as-read denied: username='{}' attempted to update notificationId={} owned by userId={}",
                    user.getUsername(), id, notification.getUserId());
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to modify this notification");
        }
        notification.setIsRead(true);
        notificationRepository.save(notification);
        logger.info("Notification marked as read: notificationId={}, userId={}", id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read"));
    }
}
