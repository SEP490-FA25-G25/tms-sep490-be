package org.fyp.tmssep490be.dtos.notification;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.Notification;
import org.fyp.tmssep490be.entities.enums.NotificationStatus;
import org.fyp.tmssep490be.entities.enums.NotificationType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

    private Long id;
    private Long recipientId;
    private String recipientName;

    private NotificationType type;
    private String title;
    private String message;

    private NotificationStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private boolean unread;

    public static NotificationDTO fromEntity(Notification notification) {
        if (notification == null) {
            return null;
        }

        return NotificationDTO.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipient() != null ? notification.getRecipient().getId() : null)
                .recipientName(notification.getRecipient() != null ? notification.getRecipient().getFullName() : null)
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .status(notification.getStatus())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .unread(notification.isUnread())
                .build();
    }
}
