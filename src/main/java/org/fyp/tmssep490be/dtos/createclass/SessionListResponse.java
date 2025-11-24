package org.fyp.tmssep490be.dtos.createclass;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionListResponse {
    private Long classId;
    private String classCode;
    private Integer totalSessions;
    private DateRangeDTO dateRange;
    private List<SessionDTO> sessions;
    private List<WeekGroupDTO> groupedByWeek;
    private List<String> warnings;

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
        private Short dayOfWeekNumber;
        private String courseSessionName;
        private String status;
        private Boolean hasTimeSlot;
        private Boolean hasResource;
        private Boolean hasTeacher;
        private Long timeSlotTemplateId;
        private Long resourceId;
        private List<Long> teacherIds;
        private TimeSlotInfoDTO timeSlotInfo;

        /**
         * Resource name assigned to this session (e.g., "Room 101", "Lab A")
         * Null if no resource assigned
         */
        private String resourceName;

        /**
         * Comma-separated teacher names (e.g., "John Smith, Lisa Chen")
         * Null if no teachers assigned
         * For quick display in session list
         */
        private String teacherName;

        /**
         * List of teachers assigned to this session
         * Used by frontend to:
         * - Show which teachers are assigned to each session
         * - Disable/lock days that are already assigned in teacher selection modal
         * - Display "Đã phân công: John Smith" message
         */
        private List<TeacherInfoDTO> teachers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlotInfoDTO {
        private String startTime;
        private String endTime;
        private String displayName;
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

    /**
     * Teacher information for session assignment tracking
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherInfoDTO {
        private Long teacherId;
        private String fullName;
        private String employeeCode;
    }
}
