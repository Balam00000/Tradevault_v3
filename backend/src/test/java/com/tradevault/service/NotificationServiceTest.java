package com.tradevault.service;

import com.tradevault.entity.Notification;
import com.tradevault.entity.enums.NotificationStatus;
import com.tradevault.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void sendNotification_success() {
        Notification expected = new Notification(1L, "Title", "Message", "LC");
        expected.setId(10L);

        when(notificationRepository.save(any(Notification.class))).thenReturn(expected);

        Notification result = notificationService.sendNotification(1L, "Title", "Message", "LC");

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Title", result.getTitle());
        assertEquals("Message", result.getMessage());
        assertEquals(com.tradevault.entity.enums.NotificationCategory.LC, result.getCategory());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void sendNotification_failure() {
        when(notificationRepository.save(any(Notification.class))).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () ->
                notificationService.sendNotification(1L, "Title", "Message", "LC"));
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void broadcastNotification_success() {
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);
        // User 2 fails, others succeed
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            if (n.getUserId().equals(2L)) {
                throw new RuntimeException("Simulated save failure");
            }
            return n;
        });

        assertDoesNotThrow(() ->
                notificationService.broadcastNotification(userIds, "Broadcast Title", "Broadcast Message", "General"));

        // Save should have been called 3 times (once for each user)
        verify(notificationRepository, times(3)).save(any(Notification.class));
    }

    @Test
    void getNotificationsForUser_success() {
        Notification notification = new Notification(1L, "Title", "Message", "LC");
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(Collections.singletonList(notification));

        List<Notification> result = notificationService.getNotificationsForUser(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void getUnreadNotifications_success() {
        Notification notification = new Notification(1L, "Title", "Message", "LC");
        when(notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(1L, NotificationStatus.UNREAD))
                .thenReturn(Collections.singletonList(notification));

        List<Notification> result = notificationService.getUnreadNotifications(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(notificationRepository).findByUserIdAndStatusOrderByCreatedAtDesc(1L, NotificationStatus.UNREAD);
    }

    @Test
    void markAsRead_success() {
        Notification notification = new Notification(1L, "Title", "Message", "LC");
        notification.setId(10L);
        notification.setIsRead(false);

        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.markAsRead(10L);

        assertTrue(notification.getIsRead());
        verify(notificationRepository).findById(10L);
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_notFound() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> notificationService.markAsRead(99L));

        verify(notificationRepository).findById(99L);
        verify(notificationRepository, never()).save(any(Notification.class));
    }
}
