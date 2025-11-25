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
public class NotificationFilterDTO {

    private NotificationStatus status;
    private NotificationType type;
    private NotificationPriority priority;
    private String referenceType;
    private Long referenceId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endDate;

    private Boolean expired;
    private Boolean unread;

    // Search by title or message
    private String search;
}