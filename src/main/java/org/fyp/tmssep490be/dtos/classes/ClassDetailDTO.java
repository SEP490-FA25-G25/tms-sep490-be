package org.fyp.tmssep490be.dtos.classes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Comprehensive Class Details DTO for Academic Affairs staff
 * Provides full information needed to review transfer requests and manage classes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassDetailDTO {

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

    // ============== Enrollment Statistics ==============
    private EnrollmentStats enrollmentStats;

    // ============== Teachers ==============
    private List<TeacherInfo> teachers;

    // ============== Session Summary ==============
    private SessionSummary sessionSummary;

    // ============== Performance Metrics ==============
    private PerformanceMetrics performanceMetrics;

    // ============== Recent Activities ==============
    private RecentActivities recentActivities;

    /**
     * Enrollment statistics for the class
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrollmentStats {
        private Integer currentEnrollment;
        private Integer maxCapacity;
        private Integer availableSlots;
        private Double occupancyRate; // Percentage
        private Integer totalEnrolledAllTime;
        private Integer transferredOut;
        private Integer dropped;
    }

    /**
     * Teacher information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherInfo {
        private Long teacherId;
        private String teacherName;
        private String teacherEmail;
        private Integer sessionsAssigned;
        private Integer sessionsCompleted;
        private LocalDate firstSessionDate;
        private LocalDate lastSessionDate;
        private Boolean isPrimaryInstructor; // Most sessions
    }

    /**
     * Session summary for the class
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
        private Integer currentCourseSessionNumber; // Where class is at in curriculum
    }

    /**
     * Performance metrics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetrics {
        private Double averageAttendanceRate;
        private Double averageAssessmentScore;
        private Integer studentsAtRisk; // Low attendance/scores
        private Integer totalAbsences;
    }

    /**
     * Recent activities related to this class
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivities {
        private List<TransferRequestSummary> recentTransferRequests;
        private Integer pendingTransferIn;
        private Integer pendingTransferOut;
        private LocalDate lastEnrollmentDate;
    }

    /**
     * Summary of transfer request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferRequestSummary {
        private Long requestId;
        private String studentName;
        private String direction; // IN or OUT
        private String status;
        private LocalDate requestedDate;
        private LocalDate effectiveDate;
    }
}
