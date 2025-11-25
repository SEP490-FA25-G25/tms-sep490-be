package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.notification.NotificationDTO;
import org.fyp.tmssep490be.dtos.notification.NotificationFilterDTO;
import org.fyp.tmssep490be.dtos.notification.NotificationRequestDTO;
import org.fyp.tmssep490be.dtos.notification.NotificationStatsDTO;
import org.fyp.tmssep490be.entities.Notification;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {

    // CRUD operations
    Notification createNotification(Long recipientId, NotificationType type, String title, String message);
    Notification createNotificationWithReference(Long recipientId, NotificationType type, String title, String message,
                                               String referenceType, Long referenceId);
    Notification createNotificationFromRequest(NotificationRequestDTO request);
    NotificationDTO updateNotification(Long notificationId, NotificationRequestDTO request);
    void deleteNotification(Long notificationId, Long userId);

    // Read operations
    NotificationDTO getNotificationById(Long notificationId, Long userId);
    Page<NotificationDTO> getUserNotifications(Long userId, NotificationFilterDTO filter, Pageable pageable);
    Page<NotificationDTO> getAllNotifications(NotificationFilterDTO filter, Pageable pageable); // Admin only

    // Status operations
    NotificationDTO markAsRead(Long notificationId, Long userId);
    int markAllAsRead(Long userId);
    int archiveNotifications(Long userId, List<Long> notificationIds);
    int deleteNotifications(Long userId, List<Long> notificationIds);

    // Statistics
    NotificationStatsDTO getUserNotificationStats(Long userId);
    NotificationStatsDTO getSystemNotificationStats(); // Admin only
    Long getUnreadCount(Long userId);

    // Bulk operations
    void sendBulkNotifications(List<Long> recipientIds, NotificationType type, String title, String message);
    void sendBulkNotificationsWithReference(List<Long> recipientIds, NotificationType type, String title, String message,
                                           String referenceType, Long referenceId);

    // Maintenance operations
    int cleanupExpiredNotifications();
    int cleanupOldReadNotifications(int daysToKeep);

    // Utility methods
    boolean hasUserNotificationForReference(Long userId, String referenceType, Long referenceId);
    Page<NotificationDTO> getNotificationsByReference(String referenceType, Long referenceId, Pageable pageable);
}