package org.fyp.tmssep490be.dtos.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO tổng hợp cho Manager Dashboard
 * Cấu trúc này được thiết kế khớp với kiểu dữ liệu mà frontend (analyticsApi.ts)
 * đang mong đợi trong ManagerDashboardResponse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDashboardDTO {

    private Summary summary;
    private List<ClassesPerBranchItem> classesPerBranch;
    private TeachingWorkload teachingWorkload;
    private List<AttendanceTrendPoint> attendanceTrend;
    private List<EnrollmentTrendPoint> enrollmentTrend;

    // ===== Summary DTOs =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private BranchSummary branches;
        private ClassSummary classes;
        private StudentSummary students;
        private TeacherSummary teachers;
        private PendingRequestSummary pendingRequests;
        private QAReportSummary qaReports;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchSummary {
        private long total;
        private long active;
        private long inactive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassSummary {
        private long activeTotal;
        private double activeChangeVsPrevRangePercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentSummary {
        private long activeTotal;
        private long newEnrollmentsInRange;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherSummary {
        private long total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingRequestSummary {
        private long totalPending;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QAReportSummary {
        private long totalInRange;
        private long needManagerReview;
    }

    // ===== Chi tiết theo chi nhánh =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassesPerBranchItem {
        private Long branchId;
        private String branchName;
        private long activeClasses;
        private boolean active;
    }

    // ===== Teaching workload =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeachingWorkload {
        private long totalTeachers;
        private long teachingTeachers;
        private long availableTeachers;
        private double teachingPercent;
        private double availablePercent;
        private double totalTeachingHoursInRange;
    }

    // ===== Xu hướng chuyên cần =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceTrendPoint {
        private String date;          // ISO yyyy-MM-dd
        private double attendanceRate; // 0-100
    }

    // ===== Xu hướng ghi danh =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrollmentTrendPoint {
        private String label;
        private String startDate;
        private String endDate;
        private long enrollments;
    }
}


