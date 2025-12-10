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

    private Map<String, Long> countsByType;

    private Long todayCount;
    private Long thisWeekCount;
    private Long thisMonthCount;
}
