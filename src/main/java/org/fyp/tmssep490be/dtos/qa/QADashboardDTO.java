package org.fyp.tmssep490be.dtos.qa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QADashboardDTO {
    private KPIMetrics kpiMetrics;
    private List<ClassRequiringAttention> classesRequiringAttention;
    private List<QAReportSummary> recentQAReports;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KPIMetrics {
        private Integer ongoingClassesCount;
        private Integer qaReportsCreatedThisMonth;
        private Double averageAttendanceRate;
        private Double averageHomeworkCompletionRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassRequiringAttention {
        private Long classId;
        private String classCode;
        private String courseName;
        private String branchName;
        private Double attendanceRate;
        private Integer qaReportCount;
        private String warningReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QAReportSummary {
        private Long reportId;
        private String reportType;
        private Long classId;
        private String classCode;
        private Long sessionId;
        private String sessionDate;
        private String status;
        private OffsetDateTime createdAt;
    }
}
