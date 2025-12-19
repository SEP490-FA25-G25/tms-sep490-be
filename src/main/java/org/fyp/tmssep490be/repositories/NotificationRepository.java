package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Notification;
import org.fyp.tmssep490be.entities.enums.NotificationStatus;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query(value = "SELECT * FROM notification n WHERE n.recipient_id = :recipientId " +
           "AND (:status IS NULL OR n.status = CAST(:status AS VARCHAR)) " +
           "AND (:type IS NULL OR n.type = CAST(:type AS VARCHAR)) " +
           "AND (:search IS NULL OR " +
           "LOWER(n.title) LIKE LOWER(CONCAT('%', CAST(:search AS VARCHAR), '%')) OR " +
           "LOWER(n.message) LIKE LOWER(CONCAT('%', CAST(:search AS VARCHAR), '%'))) " +
           "ORDER BY n.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM notification n WHERE n.recipient_id = :recipientId " +
           "AND (:status IS NULL OR n.status = CAST(:status AS VARCHAR)) " +
           "AND (:type IS NULL OR n.type = CAST(:type AS VARCHAR)) " +
           "AND (:search IS NULL OR " +
           "LOWER(n.title) LIKE LOWER(CONCAT('%', CAST(:search AS VARCHAR), '%')) OR " +
           "LOWER(n.message) LIKE LOWER(CONCAT('%', CAST(:search AS VARCHAR), '%')))",
           nativeQuery = true)
    Page<Notification> findNotificationsWithFilters(
            @Param("recipientId") Long recipientId,
            @Param("status") String status,
            @Param("type") String type,
            @Param("search") String search,
            Pageable pageable);

    Long countByRecipientIdAndStatus(Long recipientId, NotificationStatus status);

    Long countByRecipientIdAndType(Long recipientId, NotificationType type);

    List<Notification> findByRecipientIdAndStatusOrderByCreatedAtDesc(Long recipientId, NotificationStatus status);

    @Modifying
    @Query("UPDATE Notification n SET n.status = :status, n.readAt = :readAt WHERE n.recipient.id = :recipientId AND n.status = :originalStatus")
    int markAllAsRead(@Param("recipientId") Long recipientId,
                     @Param("originalStatus") NotificationStatus originalStatus,
                     @Param("status") NotificationStatus status,
                     @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'ARCHIVED' WHERE n.recipient.id = :recipientId AND n.id IN :notificationIds")
    int archiveNotifications(@Param("recipientId") Long recipientId, @Param("notificationIds") List<Long> notificationIds);

    /**
     * Check if a notification with the same title was sent to the recipient within the specified hours
     * Used to prevent duplicate reminders for the same milestone
     */
    @Query("""
            SELECT COUNT(n) > 0 FROM Notification n
            WHERE n.recipient.id = :recipientId
              AND n.title = :title
              AND n.createdAt >= :since
            """)
    boolean existsByRecipientIdAndTitleAndCreatedAtAfter(
            @Param("recipientId") Long recipientId,
            @Param("title") String title,
            @Param("since") LocalDateTime since);
}
