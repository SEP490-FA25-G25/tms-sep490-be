package org.fyp.tmssep490be.dtos.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * QA-specific Class Details DTO
 * Different from ClassDetailDTO used by Academic Affairs
 * Includes QA-specific metrics and reports
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAClassDetailDTO {

    // ============== Basic Information ==============
    private Long classId;
    private String classCode;
    private String className;
    private String courseName;
    private Long courseId;
    private String branchName;
    private Long branchId;
    private String modality;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxCapacity;
    private Integer currentEnrollment;

    // ============== Session Summary ==============
    private SessionSummary sessionSummary;

    // ============== QA Performance Metrics ==============
    private QAPerformanceMetrics performanceMetrics;

    // ============== QA Reports ==============
    private List<QAReportSummary> qaReports;

    // ============== Teachers ==============
    private List<TeacherInfo> teachers;

    /**
     * Session summary
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionSummary {
        private Integer totalSessions;
        private Integer completedSessions;
        private Integer upcomingSessions;
        private Integer cancelledSessions;
        private LocalDate nextSessionDate;
    }

    /**
     * QA-specific performance metrics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QAPerformanceMetrics {
        private Double attendanceRate;
        private Double homeworkCompletionRate;
        private Integer totalAbsences;
        private Integer studentsAtRisk;
    }

    /**
     * QA Report summary
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QAReportSummary {
        private Long reportId;
        private String reportType;
        private String reportLevel; // "Class", "Session", "Phase"
        private String status;
        private OffsetDateTime createdAt;
        private String reportedByName;
    }

    /**
     * Teacher info
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherInfo {
        private Long teacherId;
        private String teacherName;
        private Integer sessionsAssigned;
        private Integer sessionsCompleted;
    }
}
