package org.fyp.tmssep490be.dtos.notification;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.NotificationPriority;
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
    private String typeDisplayName;

    private String title;
    private String message;

    private NotificationPriority priority;
    private String priorityDisplayName;

    private NotificationStatus status;
    private String statusDisplayName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readAt;

    private String actionUrl;
    private String referenceType;
    private Long referenceId;
    private String metadata;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    private boolean expired;
    private boolean unread;

    // Static factory methods
    public static NotificationDTO fromEntity(org.fyp.tmssep490be.entities.Notification notification) {
        if (notification == null) {
            return null;
        }

        return NotificationDTO.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipient() != null ? notification.getRecipient().getId() : null)
                .recipientName(notification.getRecipient() != null ? notification.getRecipient().getFullName() : null)
                .type(notification.getType())
                .typeDisplayName(notification.getType() != null ? notification.getType().getDisplayName() : null)
                .title(notification.getTitle())
                .message(notification.getMessage())
                .priority(notification.getPriority())
                .priorityDisplayName(notification.getPriority() != null ? notification.getPriority().getDisplayName() : null)
                .status(notification.getStatus())
                .statusDisplayName(notification.getStatus() != null ? notification.getStatus().getDisplayName() : null)
                .readAt(notification.getReadAt())
                .actionUrl(notification.getActionUrl())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .metadata(notification.getMetadata())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .expiresAt(notification.getExpiresAt())
                .expired(notification.isExpired())
                .unread(notification.isUnread())
                .build();
    }
}