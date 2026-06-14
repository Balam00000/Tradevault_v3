package com.tradevault.service;

import com.tradevault.entity.Notification;
import com.tradevault.entity.enums.NotificationCategory;
import com.tradevault.entity.enums.NotificationStatus;
import com.tradevault.repository.NotificationRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link NotificationServiceImpl}.
 * This serves as a reference for writing Mockito-based service unit tests.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    // ─── Test Send Notification ──────────────────────────────────────────────

    @Test
    @DisplayName("sendNotification: should create and persist a notification successfully")
    void sendNotification_success() {
        // Arrange
        Long userId = 1L;
        String title = "LC Expiry Alert";
        String message = "Your Letter of Credit is expiring soon.";
        String category = "LC";

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(100L); // Mock the database auto-generating an ID
            return saved;
        });

        // Act
        Notification result = notificationService.sendNotification(userId, title, message, category);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getTitle()).isEqualTo(title);
        assertThat(result.getMessage()).isEqualTo(message);
        assertThat(result.getCategory()).isEqualTo(NotificationCategory.LC);
        assertThat(result.getIsRead()).isFalse();

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    // ─── Test Broadcast Notification ──────────────────────────────────────────

    @Test
    @DisplayName("broadcastNotification: should persist notifications for all specified user IDs")
    void broadcastNotification_success() {
        // Arrange
        List<Long> userIds = List.of(1L, 2L, 3L);
        String title = "System Maintenance";
        String message = "TradeVault will be down for scheduled maintenance.";
        String category = "INFO";

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        notificationService.broadcastNotification(userIds, title, message, category);

        // Assert
        verify(notificationRepository, times(3)).save(argThat(notification ->
                userIds.contains(notification.getUserId()) &&
                "System Maintenance".equals(notification.getTitle()) &&
                NotificationCategory.INFO == notification.getCategory()
        ));
    }

    // ─── Test Get Notifications For User ──────────────────────────────────────

    @Test
    @DisplayName("getNotificationsForUser: should return all notifications for the user ordered by creation date")
    void getNotificationsForUser_success() {
        // Arrange
        Long userId = 1L;
        Notification n1 = new Notification(userId, "Title 1", "Msg 1", "LC");
        Notification n2 = new Notification(userId, "Title 2", "Msg 2", "BG");
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(n1, n2));

        // Act
        List<Notification> result = notificationService.getNotificationsForUser(userId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("Title 1");
        assertThat(result.get(1).getTitle()).isEqualTo("Title 2");
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(userId);
    }

    // ─── Test Get Unread Notifications ────────────────────────────────────────

    @Test
    @DisplayName("getUnreadNotifications: should return only UNREAD notifications")
    void getUnreadNotifications_success() {
        // Arrange
        Long userId = 1L;
        Notification unread = new Notification(userId, "Unread LC", "Msg", "LC");
        when(notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, NotificationStatus.UNREAD))
                .thenReturn(List.of(unread));

        // Act
        List<Notification> result = notificationService.getUnreadNotifications(userId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Unread LC");
        verify(notificationRepository).findByUserIdAndStatusOrderByCreatedAtDesc(userId, NotificationStatus.UNREAD);
    }

    // ─── Test Mark As Read ────────────────────────────────────────────────────

    @Test
    @DisplayName("markAsRead: should update isRead status to true if notification exists")
    void markAsRead_notificationExists_updatesStatus() {
        // Arrange
        Long notificationId = 10L;
        Notification notification = new Notification(1L, "Title", "Msg", "LC");
        notification.setId(notificationId);
        notification.setIsRead(false);

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // Act
        notificationService.markAsRead(notificationId);

        // Assert
        assertThat(notification.getIsRead()).isTrue();
        verify(notificationRepository).findById(notificationId);
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("markAsRead: should do nothing if notification does not exist")
    void markAsRead_notificationNotFound_doesNothing() {
        // Arrange
        Long notificationId = 999L;
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        // Act
        notificationService.markAsRead(notificationId);

        // Assert
        verify(notificationRepository).findById(notificationId);
        verify(notificationRepository, never()).save(any());
    }
}
