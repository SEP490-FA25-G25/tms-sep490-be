package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.notification.NotificationDTO;
import org.fyp.tmssep490be.dtos.notification.NotificationFilterDTO;
import org.fyp.tmssep490be.dtos.notification.NotificationRequestDTO;
import org.fyp.tmssep490be.dtos.notification.NotificationStatsDTO;
import org.fyp.tmssep490be.entities.Notification;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.NotificationStatus;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.fyp.tmssep490be.repositories.NotificationRepository;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userAccountRepository;

    public Notification createNotification(Long recipientId, NotificationType type, String title, String message) {
        log.info("Tạo notification cho user {}: {}", recipientId, title);

        UserAccount recipient = userAccountRepository.findById(recipientId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + recipientId));

        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .status(NotificationStatus.UNREAD)
                .build();

        return notificationRepository.save(notification);
    }

    public Notification createNotificationFromRequest(NotificationRequestDTO request) {
        return createNotification(request.getRecipientId(), request.getType(), 
                                 request.getTitle(), request.getMessage());
    }

    public NotificationDTO updateNotification(Long notificationId, NotificationRequestDTO request) {
        log.info("Cập nhật notification {}: {}", notificationId, request.getTitle());

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy notification với ID: " + notificationId));

        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setType(request.getType());

        Notification saved = notificationRepository.save(notification);
        return NotificationDTO.fromEntity(saved);
    }

    public void deleteNotification(Long notificationId, Long userId) {
        log.info("Xóa notification {} cho user {}", notificationId, userId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy notification với ID: " + notificationId));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa notification này");
        }

        notificationRepository.delete(notification);
    }

    @Transactional(readOnly = true)
    public NotificationDTO getNotificationById(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy notification với ID: " + notificationId));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xem notification này");
        }

        return NotificationDTO.fromEntity(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDTO> getUserNotifications(Long userId, NotificationFilterDTO filter, Pageable pageable) {
        log.debug("Getting notifications for user {}", userId);

        String status = (filter != null && filter.getStatus() != null) ? filter.getStatus().name() : null;
        String type = (filter != null && filter.getType() != null) ? filter.getType().name() : null;
        String search = (filter != null && filter.getSearch() != null && !filter.getSearch().trim().isEmpty()) 
            ? filter.getSearch().trim() : null;

        // Native query không hỗ trợ dynamic sort từ Pageable, chỉ dùng page + size
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Notification> notifications = notificationRepository.findNotificationsWithFilters(
            userId, status, type, search, unsortedPageable);

        return notifications.map(NotificationDTO::fromEntity);
    }

    public void markAsRead(Long notificationId, Long userId) {
        log.info("Đánh dấu đã đọc notification {} cho user {}", notificationId, userId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy notification với ID: " + notificationId));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền sửa notification này");
        }

        if (notification.isUnread()) {
            notification.markAsRead();
            notificationRepository.save(notification);
        }
    }

    public int markAllAsRead(Long userId) {
        log.info("Đánh dấu đã đọc tất cả notifications cho user {}", userId);
        return notificationRepository.markAllAsRead(userId, NotificationStatus.UNREAD,
                NotificationStatus.READ, LocalDateTime.now());
    }

    public int archiveNotifications(Long userId, List<Long> notificationIds) {
        log.info("Lưu trữ {} notifications cho user {}", notificationIds.size(), userId);
        return notificationRepository.archiveNotifications(userId, notificationIds);
    }

    @Transactional(readOnly = true)
    public NotificationStatsDTO getUserNotificationStats(Long userId) {
        Long unreadCount = notificationRepository.countByRecipientIdAndStatus(userId, NotificationStatus.UNREAD);
        Long readCount = notificationRepository.countByRecipientIdAndStatus(userId, NotificationStatus.READ);
        Long archivedCount = notificationRepository.countByRecipientIdAndStatus(userId, NotificationStatus.ARCHIVED);
        Long totalCount = unreadCount + readCount + archivedCount;

        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime startOfWeek = LocalDateTime.now().minusDays(7);
        LocalDateTime startOfMonth = LocalDateTime.now().minusDays(30);

        List<Notification> allUserNotifications = notificationRepository
                .findByRecipientIdAndStatusOrderByCreatedAtDesc(userId, NotificationStatus.UNREAD);

        Long todayCount = allUserNotifications.stream()
                .filter(n -> n.getCreatedAt().isAfter(startOfDay))
                .count();
        Long thisWeekCount = allUserNotifications.stream()
                .filter(n -> n.getCreatedAt().isAfter(startOfWeek))
                .count();
        Long thisMonthCount = allUserNotifications.stream()
                .filter(n -> n.getCreatedAt().isAfter(startOfMonth))
                .count();

        Map<String, Long> countsByType = Arrays.stream(NotificationType.values())
                .collect(Collectors.toMap(
                        Enum::name,
                        type -> notificationRepository.countByRecipientIdAndType(userId, type)));

        return NotificationStatsDTO.builder()
                .totalCount(totalCount)
                .unreadCount(unreadCount)
                .readCount(readCount)
                .archivedCount(archivedCount)
                .todayCount(todayCount)
                .thisWeekCount(thisWeekCount)
                .thisMonthCount(thisMonthCount)
                .countsByType(countsByType)
                .build();
    }

    @Transactional(readOnly = true)
    public NotificationStatsDTO getSystemNotificationStats() {
        List<Notification> allNotifications = notificationRepository.findAll();

        Long totalCount = (long) allNotifications.size();
        Long unreadCount = allNotifications.stream().filter(n -> n.getStatus() == NotificationStatus.UNREAD).count();
        Long readCount = allNotifications.stream().filter(n -> n.getStatus() == NotificationStatus.READ).count();
        Long archivedCount = allNotifications.stream().filter(n -> n.getStatus() == NotificationStatus.ARCHIVED).count();

        return NotificationStatsDTO.builder()
                .totalCount(totalCount)
                .unreadCount(unreadCount)
                .readCount(readCount)
                .archivedCount(archivedCount)
                .build();
    }

    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndStatus(userId, NotificationStatus.UNREAD);
    }

    public void sendBulkNotifications(List<Long> recipientIds, NotificationType type, String title, String message) {
        log.info("Gửi bulk notifications cho {} users", recipientIds.size());

        List<UserAccount> recipients = userAccountRepository.findAllById(recipientIds);
        List<Notification> notifications = recipients.stream()
                .map(recipient -> Notification.builder()
                        .recipient(recipient)
                        .type(type)
                        .title(title)
                        .message(message)
                        .status(NotificationStatus.UNREAD)
                        .build())
                .collect(Collectors.toList());

        notificationRepository.saveAll(notifications);
    }

    public void createFeedbackReminderNotification(Long studentId, String phaseName, String subjectName, String className) {
        String title = "Nhắc nhở: Đánh giá sau phase " + phaseName;
        String message = String.format(
            "Vui lòng hoàn thành phản hồi cho môn %s - lớp %s. Phản hồi giúp cải thiện chất lượng đào tạo.",
            subjectName,
            className
        );
        
        createNotification(studentId, NotificationType.FEEDBACK_REMINDER, title, message);
    }
}
