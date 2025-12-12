package org.fyp.tmssep490be.dtos.classcreation;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSessionsOverviewDTO {
    private Long classId;
    private String classCode;
    private Integer totalSessions;
    private DateRangeDTO dateRange;
    private List<SessionDTO> sessions;
    private List<WeekGroupDTO> groupedByWeek;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateRangeDTO {
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionDTO {
        private Long sessionId;
        private Integer sequenceNumber;
        private LocalDate date;
        private String dayOfWeek;
        private Integer dayOfWeekNumber;
        private String subjectSessionName;
        private String status;
        private Boolean hasTimeSlot;
        private Boolean hasResource;
        private Boolean hasTeacher;
        private TimeSlotInfoDTO timeSlotInfo;
        private Long timeSlotTemplateId;
        private String timeSlotName;
        private String timeSlotLabel;
        private Long resourceId;
        private String resourceName;
        private String resourceDisplayName;
        private Integer weekNumber;
        private String weekRange;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlotInfoDTO {
        private Long id;
        private String name;
        private String startTime;
        private String endTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeekGroupDTO {
        private Integer weekNumber;
        private String weekRange;
        private Integer sessionCount;
        private List<Long> sessionIds;
    }
}
