package org.fyp.tmssep490be.dtos.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QAClassDetailDTO {

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

    private SessionSummary sessionSummary;
    private QAPerformanceMetrics performanceMetrics;
    private List<QAReportSummary> qaReports;
    private List<TeacherInfo> teachers;

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QAReportSummary {
        private Long reportId;
        private String reportType;
        private String reportLevel;
        private String status;
        private OffsetDateTime createdAt;
        private String reportedByName;
    }

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
