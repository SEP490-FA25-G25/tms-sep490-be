package org.fyp.tmssep490be.dtos.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO cho Center Head Dashboard.
 * Cấu trúc này được thiết kế để khớp với kiểu dữ liệu mà frontend (centerHeadApi.ts)
 * đang mong đợi trong CenterHeadDashboardResponse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CenterHeadDashboardDTO {

    private Summary summary;
    private List<ClassesPerDayItem> classesPerDay;
    private TeacherWorkloadSummary teacherWorkload;
    private List<UpcomingClassItem> upcomingClasses;
    private AttendanceSummary attendance;
    private List<AttendanceTrendPoint> attendanceTrend;

    // ===== Summary DTOs =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private ClassSummary classes;
        private StudentSummary students;
        private TeacherSummary teachers;
        private PendingReportsSummary pendingReports;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassSummary {
        private long activeTotal;
        private long upcomingThisWeek;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentSummary {
        private long activeTotal;
        private long newEnrollmentsThisWeek;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherSummary {
        private long total;
        private String scheduleStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingReportsSummary {
        private long totalPending;
        private long requiresAttention;
    }

    // ===== Classes per date (theo khoảng thời gian đã chọn) =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassesPerDayItem {
        private String date;               // ISO yyyy-MM-dd
        private long classCount;
    }

    // ===== Teacher workload =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherWorkloadSummary {
        private long totalTeachers;
        private long teachingTeachers;
        private long availableTeachers;
        private double teachingPercent;
        private double availablePercent;
        private double totalTeachingHoursThisWeek;
    }

    // ===== Upcoming classes =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpcomingClassItem {
        private Long classId;
        private String className;
        private String courseCode;
        private String startDate;   // ISO yyyy-MM-dd
        private String teacherName;
        private String roomName;
    }

    // ===== Attendance summary =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceSummary {
        private double todayRate;                // 0–100
        private long lowAttendanceClassCount;    // số lớp có tỉ lệ < ngưỡng (vd 70%)
    }

    // ===== Attendance trend =====

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceTrendPoint {
        private String date;          // ISO yyyy-MM-dd
        private double attendanceRate; // 0–100
    }
}


