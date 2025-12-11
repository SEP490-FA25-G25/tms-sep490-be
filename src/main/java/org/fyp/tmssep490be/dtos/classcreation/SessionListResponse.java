package org.fyp.tmssep490be.dtos.classcreation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO cho danh sách sessions trong class creation wizard
 */
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
        private String subjectSessionName; // Đổi từ courseSessionName
        private String status;
        private Boolean hasTimeSlot;
        private Boolean hasResource;
        private Boolean hasTeacher;
        private Long timeSlotTemplateId;
        private Long resourceId;
        private TimeSlotInfoDTO timeSlotInfo;
        private String resourceName;
        private String teacherName;
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
