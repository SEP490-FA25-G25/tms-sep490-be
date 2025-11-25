package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.notification.NotificationDTO;
import org.fyp.tmssep490be.dtos.notification.NotificationFilterDTO;
import org.fyp.tmssep490be.dtos.notification.NotificationRequestDTO;
import org.fyp.tmssep490be.dtos.notification.NotificationStatsDTO;
import org.fyp.tmssep490be.entities.Notification;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.NotificationPriority;
import org.fyp.tmssep490be.entities.enums.NotificationStatus;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.fyp.tmssep490be.repositories.NotificationRepository;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.services.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userAccountRepository;

    @Override
    public Notification createNotification(Long recipientId, NotificationType type, String title, String message) {
        log.info("Tạo notification cho user {}: {}", recipientId, title);

        UserAccount recipient = userAccountRepository.findById(recipientId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + recipientId));

        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .priority(NotificationPriority.MEDIUM)
                .status(NotificationStatus.UNREAD)
                .build();

        return notificationRepository.save(notification);
    }

    @Override
    public Notification createNotificationWithReference(Long recipientId, NotificationType type, String title, String message,
                                                     String referenceType, Long referenceId) {
        log.info("Tạo notification với reference cho user {}: {} - {}:{}", recipientId, title, referenceType, referenceId);

        UserAccount recipient = userAccountRepository.findById(recipientId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + recipientId));

        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .priority(NotificationPriority.MEDIUM)
                .status(NotificationStatus.UNREAD)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build();

        return notificationRepository.save(notification);
    }

    @Override
    public Notification createNotificationFromRequest(NotificationRequestDTO request) {
        log.info("Tạo notification từ request cho user {}: {}", request.getRecipientId(), request.getTitle());

        UserAccount recipient = userAccountRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + request.getRecipientId()));

        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .priority(request.getPriority() != null ? request.getPriority() : NotificationPriority.MEDIUM)
                .status(NotificationStatus.UNREAD)
                .actionUrl(request.getActionUrl())
                .referenceType(request.getReferenceType())
                .referenceId(request.getReferenceId())
                .metadata(request.getMetadata())
                .expiresAt(request.getExpiresAt())
                .build();

        return notificationRepository.save(notification);
    }

    @Override
    public NotificationDTO updateNotification(Long notificationId, NotificationRequestDTO request) {
        log.info("Cập nhật notification {}: {}", notificationId, request.getTitle());

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy notification với ID: " + notificationId));

        // Update fields
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        if (request.getPriority() != null) {
            notification.setPriority(request.getPriority());
        }
        notification.setActionUrl(request.getActionUrl());
        notification.setReferenceType(request.getReferenceType());
        notification.setReferenceId(request.getReferenceId());
        notification.setMetadata(request.getMetadata());
        notification.setExpiresAt(request.getExpiresAt());

        Notification saved = notificationRepository.save(notification);
        return NotificationDTO.fromEntity(saved);
    }

    @Override
    public void deleteNotification(Long notificationId, Long userId) {
        log.info("Xóa notification {} cho user {}", notificationId, userId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy notification với ID: " + notificationId));

        // Check if user owns this notification
        if (!notification.getRecipient().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa notification này");
        }

        notificationRepository.delete(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationDTO getNotificationById(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy notification với ID: " + notificationId));

        // Check if user owns this notification
        if (!notification.getRecipient().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xem notification này");
        }

        return NotificationDTO.fromEntity(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getUserNotifications(Long userId, NotificationFilterDTO filter, Pageable pageable) {
        Specification<Notification> spec = buildUserNotificationSpecification(userId, filter);
        Page<Notification> notifications = notificationRepository.findAll(spec, pageable);
        return notifications.map(NotificationDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getAllNotifications(NotificationFilterDTO filter, Pageable pageable) {
        Specification<Notification> spec = buildNotificationSpecification(filter);
        Page<Notification> notifications = notificationRepository.findAll(spec, pageable);
        return notifications.map(NotificationDTO::fromEntity);
    }

    @Override
    public NotificationDTO markAsRead(Long notificationId, Long userId) {
        log.info("Đánh dấu đã đọc notification {} cho user {}", notificationId, userId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy notification với ID: " + notificationId));

        // Check if user owns this notification
        if (!notification.getRecipient().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền sửa notification này");
        }

        if (notification.isUnread()) {
            notification.markAsRead();
            notificationRepository.save(notification);
        }

        return NotificationDTO.fromEntity(notification);
    }

    @Override
    public int markAllAsRead(Long userId) {
        log.info("Đánh dấu đã đọc tất cả notifications cho user {}", userId);

        return notificationRepository.markAllAsRead(
                userId,
                NotificationStatus.UNREAD,
                NotificationStatus.READ,
                LocalDateTime.now()
        );
    }

    @Override
    public int archiveNotifications(Long userId, List<Long> notificationIds) {
        log.info("Lưu trữ {} notifications cho user {}", notificationIds.size(), userId);

        return notificationRepository.archiveNotifications(userId, notificationIds);
    }

    @Override
    public int deleteNotifications(Long userId, List<Long> notificationIds) {
        log.info("Xóa {} notifications cho user {}", notificationIds.size(), userId);

        return notificationRepository.deleteNotifications(userId, NotificationStatus.READ, notificationIds);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationStatsDTO getUserNotificationStats(Long userId) {
        Long totalCount = notificationRepository.countByRecipientIdAndStatus(userId, NotificationStatus.UNREAD);
        Long unreadCount = notificationRepository.countByRecipientIdAndStatus(userId, NotificationStatus.UNREAD);
        Long readCount = notificationRepository.countByRecipientIdAndStatus(userId, NotificationStatus.READ);
        Long archivedCount = notificationRepository.countByRecipientIdAndStatus(userId, NotificationStatus.ARCHIVED);

        // Get counts by type
        Map<String, Long> countsByType = Arrays.stream(NotificationType.values())
                .collect(Collectors.toMap(
                        type -> type.getDisplayName(),
                        type -> notificationRepository.countByRecipientIdAndType(userId, type)
                ));

        // Get counts by priority
        Map<String, Long> countsByPriority = Map.of(
                "Thấp", 0L,
                "Trung bình", 0L,
                "Cao", 0L,
                "Khẩn cấp", 0L
        );

        // Get recent activity
        LocalDateTime now = LocalDateTime.now();
        Long todayCount = getUserNotificationsCount(userId, now.toLocalDate().atStartOfDay(), now);
        Long thisWeekCount = getUserNotificationsCount(userId, now.minusDays(7), now);
        Long thisMonthCount = getUserNotificationsCount(userId, now.minusDays(30), now);

        return NotificationStatsDTO.builder()
                .totalCount(totalCount + readCount + archivedCount)
                .unreadCount(unreadCount)
                .readCount(readCount)
                .archivedCount(archivedCount)
                .expiredCount(0L) // TODO: Implement expired count
                .countsByType(countsByType)
                .countsByPriority(countsByPriority)
                .todayCount(todayCount)
                .thisWeekCount(thisWeekCount)
                .thisMonthCount(thisMonthCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationStatsDTO getSystemNotificationStats() {
        // Get all notifications count
        long totalActive = notificationRepository.count(); // This should be filtered for non-archived

        // TODO: Implement more comprehensive system stats

        return NotificationStatsDTO.builder()
                .totalActiveNotifications(totalActive)
                .totalExpiredNotifications(0L) // TODO: Implement expired count
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndStatus(userId, NotificationStatus.UNREAD);
    }

    @Override
    public void sendBulkNotifications(List<Long> recipientIds, NotificationType type, String title, String message) {
        log.info("Gửi bulk notifications cho {} users", recipientIds.size());

        List<UserAccount> recipients = userAccountRepository.findAllById(recipientIds);
        List<Notification> notifications = recipients.stream()
                .map(recipient -> Notification.builder()
                        .recipient(recipient)
                        .type(type)
                        .title(title)
                        .message(message)
                        .priority(NotificationPriority.MEDIUM)
                        .status(NotificationStatus.UNREAD)
                        .build())
                .collect(Collectors.toList());

        notificationRepository.saveAll(notifications);
    }

    @Override
    public void sendBulkNotificationsWithReference(List<Long> recipientIds, NotificationType type, String title, String message,
                                                  String referenceType, Long referenceId) {
        log.info("Gửi bulk notifications với reference cho {} users", recipientIds.size());

        List<UserAccount> recipients = userAccountRepository.findAllById(recipientIds);
        List<Notification> notifications = recipients.stream()
                .map(recipient -> Notification.builder()
                        .recipient(recipient)
                        .type(type)
                        .title(title)
                        .message(message)
                        .priority(NotificationPriority.MEDIUM)
                        .status(NotificationStatus.UNREAD)
                        .referenceType(referenceType)
                        .referenceId(referenceId)
                        .build())
                .collect(Collectors.toList());

        notificationRepository.saveAll(notifications);
    }

    @Override
    public int cleanupExpiredNotifications() {
        log.info("Dọn dẹp notifications đã hết hạn");

        return notificationRepository.deleteExpiredNotifications(LocalDateTime.now());
    }

    @Override
    public int cleanupOldReadNotifications(int daysToKeep) {
        log.info("Dọn dẹp notifications đã đọc cũ hơn {} ngày", daysToKeep);

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        return notificationRepository.deleteOldReadNotifications(cutoffDate);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserNotificationForReference(Long userId, String referenceType, Long referenceId) {
        return notificationRepository.existsByRecipientIdAndReferenceTypeAndReferenceId(userId, referenceType, referenceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotificationsByReference(String referenceType, Long referenceId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(
                referenceType, referenceId, pageable);
        return notifications.map(NotificationDTO::fromEntity);
    }

    // Helper methods
    private Specification<Notification> buildUserNotificationSpecification(Long userId, NotificationFilterDTO filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter by recipient
            predicates.add(criteriaBuilder.equal(root.get("recipient").get("id"), userId));

            if (filter != null) {
                if (filter.getStatus() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
                }

                if (filter.getType() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("type"), filter.getType()));
                }

                if (filter.getPriority() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("priority"), filter.getPriority()));
                }

                if (filter.getReferenceType() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("referenceType"), filter.getReferenceType()));
                }

                if (filter.getReferenceId() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("referenceId"), filter.getReferenceId()));
                }

                if (filter.getStartDate() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filter.getStartDate()));
                }

                if (filter.getEndDate() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), filter.getEndDate()));
                }

                if (filter.getSearch() != null && !filter.getSearch().trim().isEmpty()) {
                    String searchPattern = "%" + filter.getSearch().toLowerCase() + "%";
                    Predicate titlePredicate = criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("title")), searchPattern);
                    Predicate messagePredicate = criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("message")), searchPattern);
                    predicates.add(criteriaBuilder.or(titlePredicate, messagePredicate));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<Notification> buildNotificationSpecification(NotificationFilterDTO filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter != null) {
                if (filter.getStatus() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("status"), filter.getStatus()));
                }

                if (filter.getType() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("type"), filter.getType()));
                }

                if (filter.getPriority() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("priority"), filter.getPriority()));
                }

                if (filter.getReferenceType() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("referenceType"), filter.getReferenceType()));
                }

                if (filter.getReferenceId() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("referenceId"), filter.getReferenceId()));
                }

                if (filter.getStartDate() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filter.getStartDate()));
                }

                if (filter.getEndDate() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), filter.getEndDate()));
                }

                if (filter.getSearch() != null && !filter.getSearch().trim().isEmpty()) {
                    String searchPattern = "%" + filter.getSearch().toLowerCase() + "%";
                    Predicate titlePredicate = criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("title")), searchPattern);
                    Predicate messagePredicate = criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("message")), searchPattern);
                    predicates.add(criteriaBuilder.or(titlePredicate, messagePredicate));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Long getUserNotificationsCount(Long userId, LocalDateTime start, LocalDateTime end) {
        // This is a simplified implementation
        // In a real application, you'd create a custom repository method for this
        return 0L; // Placeholder
    }
}