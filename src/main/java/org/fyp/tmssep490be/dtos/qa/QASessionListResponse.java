package org.fyp.tmssep490be.dtos.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Session List Response with QA metrics
 * Used by Academic Affairs and Teachers to monitor class sessions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QASessionListResponse {

    private Long classId;
    private String classCode;
    private Integer totalSessions;
    private List<QASessionItemDTO> sessions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QASessionItemDTO {
        private Long sessionId;
        private Integer sequenceNumber;
        private LocalDate date;
        private String dayOfWeek;
        private String timeSlot;
        private String topic;
        private String status;
        private String teacherName;

        // QA-specific metrics
        private Integer totalStudents;
        private Integer presentCount;
        private Integer absentCount;
        private Double attendanceRate;
        private Integer homeworkCompletedCount;
        private Double homeworkCompletionRate;

        // QA report status
        private Boolean hasQAReport;
        private Integer qaReportCount;
    }
}
