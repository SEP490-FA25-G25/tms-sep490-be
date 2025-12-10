package org.fyp.tmssep490be.dtos.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.NotificationStatus;
import org.fyp.tmssep490be.entities.enums.NotificationType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationFilterDTO {

    private NotificationStatus status;
    private NotificationType type;
    private String search;
}
