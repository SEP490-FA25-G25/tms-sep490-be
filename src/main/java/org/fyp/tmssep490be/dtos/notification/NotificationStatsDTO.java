package org.fyp.tmssep490be.dtos.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatsDTO {

    private Long totalCount;
    private Long unreadCount;
    private Long readCount;
    private Long archivedCount;
    private Long expiredCount;

    // Counts by type
    private Map<String, Long> countsByType;

    // Counts by priority
    private Map<String, Long> countsByPriority;

    // Recent activity
    private Long todayCount;
    private Long thisWeekCount;
    private Long thisMonthCount;

    // Stats for admin
    private Long totalActiveNotifications;
    private Long totalExpiredNotifications;
}