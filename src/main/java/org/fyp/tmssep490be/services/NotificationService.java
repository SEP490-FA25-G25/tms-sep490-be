package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.Notification;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.NotificationPriority;
import org.fyp.tmssep490be.entities.enums.NotificationStatus;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.fyp.tmssep490be.repositories.NotificationRepository;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationService {

    private final UserAccountRepository userAccountRepository;
    private final NotificationRepository notificationRepository;

    public void createNotificationWithReference(Long recipientId, NotificationType type, String title,
                                                String message,
                                                String referenceType, Long referenceId) {
        log.info("Tạo notification với reference cho user {}: {} - {}:{}", recipientId, title, referenceType,
                referenceId);

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

        notificationRepository.save(notification);
    }

    public void sendBulkNotificationsWithReference(List<Long> recipientIds, NotificationType type, String title,
                                                   String message,
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

}
