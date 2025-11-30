package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Notification;
import org.fyp.tmssep490be.entities.enums.NotificationPriority;
import org.fyp.tmssep490be.entities.enums.NotificationStatus;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    // User notifications with pagination
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    // Filter by status
    Page<Notification> findByRecipientIdAndStatusOrderByCreatedAtDesc(Long recipientId, NotificationStatus status, Pageable pageable);

    // Filter by type
    Page<Notification> findByRecipientIdAndTypeOrderByCreatedAtDesc(Long recipientId, NotificationType type, Pageable pageable);

    // Filter by status and type
    Page<Notification> findByRecipientIdAndStatusAndTypeOrderByCreatedAtDesc(Long recipientId, NotificationStatus status, NotificationType type, Pageable pageable);

    // Unread count
    Long countByRecipientIdAndStatus(Long recipientId, NotificationStatus status);

    // Count by type for a user
    Long countByRecipientIdAndType(Long recipientId, NotificationType type);

    // Find unread notifications
    List<Notification> findByRecipientIdAndStatusOrderByCreatedAtDesc(Long recipientId, NotificationStatus status);

    // Find by reference
    Page<Notification> findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(String referenceType, Long referenceId, Pageable pageable);

    // Bulk operations
    @Modifying
    @Query("UPDATE Notification n SET n.status = :status, n.readAt = :readAt WHERE n.recipient.id = :recipientId AND n.status = :originalStatus")
    int markAllAsRead(@Param("recipientId") Long recipientId,
                     @Param("originalStatus") NotificationStatus originalStatus,
                     @Param("status") NotificationStatus status,
                     @Param("readAt") LocalDateTime readAt);

    // Archive notifications
    @Modifying
    @Query("UPDATE Notification n SET n.status = 'ARCHIVED' WHERE n.recipient.id = :recipientId AND n.id IN :notificationIds")
    int archiveNotifications(@Param("recipientId") Long recipientId, @Param("notificationIds") List<Long> notificationIds);

    // Delete/archived notifications
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.recipient.id = :recipientId AND n.status = :status AND n.id IN :notificationIds")
    int deleteNotifications(@Param("recipientId") Long recipientId,
                           @Param("status") NotificationStatus status,
                           @Param("notificationIds") List<Long> notificationIds);

    // Cleanup expired notifications
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.expiresAt < :now")
    int deleteExpiredNotifications(@Param("now") LocalDateTime now);

    // Cleanup old read notifications (for maintenance)
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.status = 'READ' AND n.readAt < :cutoffDate")
    int deleteOldReadNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Find notifications by priority and status
    Page<Notification> findByRecipientIdAndPriorityAndStatusOrderByCreatedAtDesc(Long recipientId, NotificationPriority priority, NotificationStatus status, Pageable pageable);

    // Check if user has notification for specific reference
    boolean existsByRecipientIdAndReferenceTypeAndReferenceId(Long recipientId, String referenceType, Long referenceId);

    // Check if user has notification for specific reference with metadata containing a string
    @Query(value = "SELECT COUNT(n) > 0 FROM notification n WHERE n.recipient_id = :recipientId " +
           "AND n.reference_type = :referenceType AND n.reference_id = :referenceId " +
           "AND CAST(n.metadata AS TEXT) LIKE CONCAT('%', :metadataContains, '%')", nativeQuery = true)
    boolean existsByRecipientIdAndReferenceTypeAndReferenceIdAndMetadataContaining(
        @Param("recipientId") Long recipientId,
        @Param("referenceType") String referenceType,
        @Param("referenceId") Long referenceId,
        @Param("metadataContains") String metadataContains);
}